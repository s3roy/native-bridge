import Foundation
import AVFoundation
import Contacts
import ContactsUI
import CoreLocation
import Photos
import PhotosUI
import UIKit

enum BridgeDevice {

    // MARK: - Capabilities

    static func getCapabilities() -> [String: Any] {
        [
            "platform": "ios",
            "location": CLLocationManager.locationServicesEnabled(),
            "camera": UIImagePickerController.isSourceTypeAvailable(.camera),
            "gallery": true,
            "contacts": true,
            "clipboard": true,
            "share": true,
            "vibrate": true,
            "dial": true,
            "sms": true,
            "maps": true,
            "torch": AVCaptureDevice.default(for: .video)?.hasTorch ?? false,
            "getUserMedia": true,
        ]
    }

    // MARK: - Clipboard

    static func getClipboard() -> [String: Any] {
        ["text": UIPasteboard.general.string ?? ""]
    }

    static func setClipboard(_ text: String) -> [String: Any] {
        UIPasteboard.general.string = text
        return ["set": true]
    }

    // MARK: - Haptics / vibrate

    static func vibrate(_ params: [String: Any]) -> [String: Any] {
        if let pattern = params["pattern"] as? [NSNumber], !pattern.isEmpty {
            let gen = UIImpactFeedbackGenerator(style: .medium)
            gen.prepare()
            for (i, n) in pattern.enumerated() {
                DispatchQueue.main.asyncAfter(deadline: .now() + Double(truncating: n) / 1000.0 * Double(i)) {
                    gen.impactOccurred()
                }
            }
        } else {
            let gen = UIImpactFeedbackGenerator(style: .medium)
            gen.prepare()
            gen.impactOccurred()
        }
        return ["vibrated": true]
    }

    // MARK: - Dial / SMS / Maps

    static func dial(_ phone: String) -> [String: Any] {
        BridgeIntents.openUrl("tel:\(phone)")
    }

    static func sendSms(phone: String, body: String) -> [String: Any] {
        var c = URLComponents()
        c.scheme = "sms"
        c.path = phone
        c.queryItems = body.isEmpty ? nil : [URLQueryItem(name: "body", value: body)]
        guard let url = c.url?.absoluteString else {
            return ["opened": false, "error": "Invalid phone"]
        }
        return BridgeIntents.openUrl(url)
    }

    static func openMaps(_ params: [String: Any]) -> [String: Any] {
        if let lat = params["latitude"] as? Double, let lng = params["longitude"] as? Double {
            let q = (params["query"] as? String) ?? "\(lat),\(lng)"
            let encoded = q.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? q
            return BridgeIntents.openUrl("http://maps.apple.com/?ll=\(lat),\(lng)&q=\(encoded)")
        }
        if let query = params["query"] as? String, !query.isEmpty {
            let encoded = query.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? query
            return BridgeIntents.openUrl("http://maps.apple.com/?q=\(encoded)")
        }
        return ["opened": false, "error": "latitude/longitude or query required"]
    }

    static func setTorch(_ enabled: Bool) -> [String: Any] {
        if BridgeDeviceSession.isBusy(.camera) {
            return ["enabled": false, "error": "Camera is in use"]
        }
        guard let device = AVCaptureDevice.default(for: .video), device.hasTorch else {
            return ["enabled": false, "error": "Torch unavailable"]
        }
        do {
            try device.lockForConfiguration()
            if enabled { try device.setTorchModeOn(level: 1.0) }
            else { device.torchMode = .off }
            device.unlockForConfiguration()
            return ["enabled": enabled]
        } catch {
            return ["enabled": false, "error": error.localizedDescription]
        }
    }

    static func releaseTorch() {
        _ = setTorch(false)
    }

    static func getDeviceResourceStatus() -> [String: Any] {
        BridgeDeviceSession.status()
    }

    static func releaseDeviceResources(_ params: [String: Any]) -> [String: Any] {
        if let list = params["resources"] as? [String] {
            for name in list {
                switch name {
                case "camera": BridgeDeviceSession.cancel(.camera)
                case "gallery": BridgeDeviceSession.cancel(.gallery)
                case "contact": BridgeDeviceSession.cancel(.contact)
                case "location": BridgeDeviceSession.cancel(.location)
                default: break
                }
            }
        } else {
            BridgeDeviceSession.releaseAll()
        }
        return ["released": true, "status": BridgeDeviceSession.status()]
    }

    static func cancelDeviceOperation(_ params: [String: Any]) -> [String: Any] {
        let type = (params["type"] as? String) ?? (params["resource"] as? String) ?? ""
        let resource: BridgeDeviceSession.Resource?
        switch type {
        case "camera": resource = .camera
        case "gallery": resource = .gallery
        case "contact": resource = .contact
        case "location": resource = .location
        default: resource = nil
        }
        guard let resource else {
            return ["cancelled": false, "error": "Unknown resource: \(type)"]
        }
        BridgeDeviceSession.cancel(resource)
        DevicePickerCoordinator.shared.cancelIfNeeded()
        DeviceContactCoordinator.shared.cancelIfNeeded()
        return ["cancelled": true, "status": BridgeDeviceSession.status()]
    }

    // MARK: - Location

    static func getCurrentLocation(
        _ params: [String: Any],
        completion: @escaping ([String: Any]?, Error?) -> Void
    ) {
        guard BridgeDeviceSession.tryOccupy(.location) else {
            completion(nil, BridgeDeviceError.busy("location"))
            return
        }
        let status = CLLocationManager.authorizationStatus()
        guard status == .authorizedWhenInUse || status == .authorizedAlways else {
            BridgeDeviceSession.release(.location)
            completion(nil, BridgeDeviceError.permissionDenied("location"))
            return
        }
        DeviceLocationCoordinator.shared.fetch(params: params) { result, error in
            BridgeDeviceSession.release(.location)
            completion(result, error)
        }
    }

    // MARK: - Camera / gallery / contacts / share

    static func takePhoto(
        _ params: [String: Any],
        completion: @escaping ([String: Any]?, Error?) -> Void
    ) {
        guard BridgeDeviceSession.tryOccupy(.camera) else {
            completion(nil, BridgeDeviceError.busy("camera")); return
        }
        guard UIImagePickerController.isSourceTypeAvailable(.camera) else {
            BridgeDeviceSession.release(.camera)
            completion(nil, BridgeDeviceError.unavailable("camera"))
            return
        }
        guard AVCaptureDevice.authorizationStatus(for: .video) == .authorized else {
            BridgeDeviceSession.release(.camera)
            completion(nil, BridgeDeviceError.permissionDenied("camera"))
            return
        }
        DevicePickerCoordinator.shared.present(
            source: .camera,
            resource: .camera,
            params: params,
            completion: completion
        )
    }

    static func pickImage(
        _ params: [String: Any],
        completion: @escaping ([String: Any]?, Error?) -> Void
    ) {
        if params["multiple"] as? Bool == true {
            pickImages(params, completion: completion)
            return
        }
        guard BridgeDeviceSession.tryOccupy(.gallery) else {
            completion(nil, BridgeDeviceError.busy("gallery")); return
        }
        if #available(iOS 14.0, *), false {
            // single via PHPicker optional
        }
        DevicePickerCoordinator.shared.present(
            source: .photoLibrary,
            resource: .gallery,
            params: params,
            completion: completion
        )
    }

    static func pickImages(
        _ params: [String: Any],
        completion: @escaping ([String: Any]?, Error?) -> Void
    ) {
        guard BridgeDeviceSession.tryOccupy(.gallery) else {
            completion(nil, BridgeDeviceError.busy("gallery")); return
        }
        if #available(iOS 14.0, *) {
            DevicePhotoPickerCoordinator.shared.present(params: params, completion: completion)
        } else {
            BridgeDeviceSession.release(.gallery)
            completion(nil, BridgeDeviceError.unavailable("multi pick requires iOS 14+"))
        }
    }

    static func pickContact(completion: @escaping ([String: Any]?, Error?) -> Void) {
        guard BridgeDeviceSession.tryOccupy(.contact) else {
            completion(nil, BridgeDeviceError.busy("contact")); return
        }
        DeviceContactCoordinator.shared.present(completion: completion)
    }

    static func share(_ params: [String: Any], completion: @escaping ([String: Any]?, Error?) -> Void) {
        let text = params["text"] as? String ?? ""
        let url = params["url"] as? String ?? ""
        var items: [Any] = []
        if !text.isEmpty { items.append(text) }
        if !url.isEmpty, let u = URL(string: url) { items.append(u) }
        guard !items.isEmpty else {
            completion(nil, BridgeDeviceError.invalidParams("text or url required"))
            return
        }
        guard let vc = BridgePresenter.topViewController() else {
            completion(nil, BridgeDeviceError.noPresenter)
            return
        }
        let activity = UIActivityViewController(activityItems: items, applicationActivities: nil)
        activity.completionWithItemsHandler = { _, _, _, _ in
            completion(["shared": true], nil)
        }
        DispatchQueue.main.async {
            vc.present(activity, animated: true)
        }
    }

    // MARK: - Image encoding

    static func encodeImage(_ image: UIImage, quality: Int, maxDim: CGFloat = 1920) -> [String: Any]? {
        let scaled = scaleImage(image, maxDim: maxDim)
        guard let data = scaled.jpegData(compressionQuality: CGFloat(quality) / 100.0) else {
            return nil
        }
        return [
            "base64": data.base64EncodedString(),
            "mimeType": "image/jpeg",
            "width": Int(scaled.size.width),
            "height": Int(scaled.size.height),
        ]
    }

    private static func scaleImage(_ image: UIImage, maxDim: CGFloat) -> UIImage {
        let w = image.size.width
        let h = image.size.height
        guard max(w, h) > maxDim else { return image }
        let scale = maxDim / max(w, h)
        let size = CGSize(width: w * scale, height: h * scale)
        UIGraphicsBeginImageContextWithOptions(size, false, 1.0)
        image.draw(in: CGRect(origin: .zero, size: size))
        let result = UIGraphicsGetImageFromCurrentImageContext() ?? image
        UIGraphicsEndImageContext()
        return result
    }

    static func locationDict(_ loc: CLLocation) -> [String: Any] {
        [
            "latitude": loc.coordinate.latitude,
            "longitude": loc.coordinate.longitude,
            "accuracy": loc.horizontalAccuracy,
            "altitude": loc.altitude,
            "heading": loc.course,
            "speed": loc.speed,
            "timestamp": loc.timestamp.timeIntervalSince1970 * 1000,
        ]
    }
}

enum BridgeDeviceError: LocalizedError {
    case permissionDenied(String)
    case unavailable(String)
    case invalidParams(String)
    case noPresenter
    case cancelled
    case busy(String)

    var errorDescription: String? {
        switch self {
        case .permissionDenied(let p): return "\(p) permission not granted"
        case .unavailable(let f): return "\(f) unavailable"
        case .invalidParams(let m): return m
        case .noPresenter: return "Cannot present UI"
        case .cancelled: return "Cancelled"
        case .busy(let r): return "\(r) is busy"
        }
    }
}

// MARK: - Presenter

enum BridgePresenter {
    static func topViewController() -> UIViewController? {
        guard let scene = UIApplication.shared.connectedScenes
            .compactMap({ $0 as? UIWindowScene })
            .first(where: { $0.activationState == .foregroundActive }),
              let root = scene.windows.first(where: { $0.isKeyWindow })?.rootViewController
        else { return nil }
        return topMost(from: root)
    }

    private static func topMost(from vc: UIViewController) -> UIViewController {
        if let presented = vc.presentedViewController {
            return topMost(from: presented)
        }
        if let nav = vc as? UINavigationController, let visible = nav.visibleViewController {
            return topMost(from: visible)
        }
        if let tab = vc as? UITabBarController, let selected = tab.selectedViewController {
            return topMost(from: selected)
        }
        return vc
    }
}

// MARK: - Location coordinator

private final class DeviceLocationCoordinator: NSObject, CLLocationManagerDelegate {
    static let shared = DeviceLocationCoordinator()
    private var manager: CLLocationManager?
    private var completion: (([String: Any]?, Error?) -> Void)?
    private var timeoutWork: DispatchWorkItem?

    func fetch(params: [String: Any], completion: @escaping ([String: Any]?, Error?) -> Void) {
        self.completion = completion
        let mgr = CLLocationManager()
        manager = mgr
        mgr.delegate = self
        let high = params["enableHighAccuracy"] as? Bool ?? true
        mgr.desiredAccuracy = high ? kCLLocationAccuracyBest : kCLLocationAccuracyKilometer
        BridgeDeviceSession.registerLocationCancel { [weak self] in
            self?.finish(["cancelled": true], nil)
        }
        mgr.requestLocation()
        let timeout = (params["timeout"] as? Double ?? 15_000) / 1000.0
        timeoutWork?.cancel()
        let work = DispatchWorkItem { [weak self] in
            self?.finish(nil, BridgeDeviceError.unavailable("location timeout"))
        }
        timeoutWork = work
        DispatchQueue.main.asyncAfter(deadline: .now() + timeout, execute: work)
    }

    func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        guard let loc = locations.last else { return }
        finish(BridgeDevice.locationDict(loc), nil)
    }

    func locationManager(_ manager: CLLocationManager, didFailWithError error: Error) {
        finish(nil, error)
    }

    private func finish(_ result: [String: Any]?, _ error: Error?) {
        timeoutWork?.cancel()
        timeoutWork = nil
        BridgeDeviceSession.clearLocationCancel()
        let cb = completion
        completion = nil
        manager = nil
        DispatchQueue.main.async { cb?(result, error) }
    }
}

// MARK: - Image picker

private final class DevicePickerCoordinator: NSObject,
    UIImagePickerControllerDelegate, UINavigationControllerDelegate
{
    static let shared = DevicePickerCoordinator()
    private var completion: (([String: Any]?, Error?) -> Void)?
    private var quality = 80
    private var maxDim: CGFloat = 1920
    private var resource: BridgeDeviceSession.Resource = .gallery
    private weak var presentedPicker: UIImagePickerController?

    func present(
        source: UIImagePickerController.SourceType,
        resource: BridgeDeviceSession.Resource,
        params: [String: Any],
        completion: @escaping ([String: Any]?, Error?) -> Void
    ) {
        guard presentedPicker == nil else {
            BridgeDeviceSession.release(resource)
            completion(nil, BridgeDeviceError.busy(resource.rawValue))
            return
        }
        guard let vc = BridgePresenter.topViewController() else {
            BridgeDeviceSession.release(resource)
            completion(nil, BridgeDeviceError.noPresenter)
            return
        }
        self.completion = completion
        self.resource = resource
        quality = params["quality"] as? Int ?? 80
        maxDim = CGFloat(params["maxDimension"] as? Int ?? 1920)
        let picker = UIImagePickerController()
        picker.sourceType = source
        picker.delegate = self
        presentedPicker = picker
        if source == .camera {
            picker.cameraDevice = (params["facing"] as? String) == "front"
                ? .front : .rear
        }
        DispatchQueue.main.async { vc.present(picker, animated: true) }
    }

    func cancelIfNeeded() {
        presentedPicker?.dismiss(animated: true)
        presentedPicker = nil
    }

    func imagePickerControllerDidCancel(_ picker: UIImagePickerController) {
        picker.dismiss(animated: true) { [weak self] in
            self?.presentedPicker = nil
            self?.finish(["cancelled": true], nil)
        }
    }

    func imagePickerController(
        _ picker: UIImagePickerController,
        didFinishPickingMediaWithInfo info: [UIImagePickerController.InfoKey: Any]
    ) {
        picker.dismiss(animated: true) { [weak self] in
            guard let self else { return }
            self.presentedPicker = nil
            guard let image = info[.originalImage] as? UIImage,
                  let encoded = BridgeDevice.encodeImage(image, quality: self.quality, maxDim: self.maxDim)
            else {
                self.finish(nil, BridgeDeviceError.unavailable("image"))
                return
            }
            self.finish(encoded, nil)
        }
    }

    private func finish(_ result: [String: Any]?, _ error: Error?) {
        BridgeDeviceSession.release(resource)
        let cb = completion
        completion = nil
        cb?(result, error)
    }
}

@available(iOS 14.0, *)
private final class DevicePhotoPickerCoordinator: NSObject, PHPickerViewControllerDelegate {
    static let shared = DevicePhotoPickerCoordinator()
    private var completion: (([String: Any]?, Error?) -> Void)?
    private var quality = 80
    private var maxDim: CGFloat = 1920

    func present(params: [String: Any], completion: @escaping ([String: Any]?, Error?) -> Void) {
        guard let vc = BridgePresenter.topViewController() else {
            BridgeDeviceSession.release(.gallery)
            completion(nil, BridgeDeviceError.noPresenter)
            return
        }
        self.completion = completion
        quality = params["quality"] as? Int ?? 80
        maxDim = CGFloat(params["maxDimension"] as? Int ?? 1920)
        let maxCount = min(params["maxCount"] as? Int ?? 10, 20)
        var config = PHPickerConfiguration()
        config.filter = .images
        config.selectionLimit = maxCount
        let picker = PHPickerViewController(configuration: config)
        picker.delegate = self
        DispatchQueue.main.async { vc.present(picker, animated: true) }
    }

    func picker(_ picker: PHPickerViewController, didFinishPicking results: [PHPickerResult]) {
        picker.dismiss(animated: true)
        if results.isEmpty {
            finish(["cancelled": true], nil)
            return
        }
        let group = DispatchGroup()
        var images: [[String: Any]] = []
        let lock = NSLock()
        for result in results {
            group.enter()
            result.itemProvider.loadObject(ofClass: UIImage.self) { object, _ in
                defer { group.leave() }
                guard let image = object as? UIImage,
                      let encoded = BridgeDevice.encodeImage(image, quality: self.quality, maxDim: self.maxDim)
                else { return }
                lock.lock()
                images.append(encoded)
                lock.unlock()
            }
        }
        group.notify(queue: .main) { [weak self] in
            if images.isEmpty {
                self?.finish(nil, BridgeDeviceError.unavailable("image"))
            } else {
                self?.finish(["images": images, "count": images.count], nil)
            }
        }
    }

    private func finish(_ result: [String: Any]?, _ error: Error?) {
        BridgeDeviceSession.release(.gallery)
        let cb = completion
        completion = nil
        cb?(result, error)
    }
}

// MARK: - Contact picker

private final class DeviceContactCoordinator: NSObject, CNContactPickerDelegate {
    static let shared = DeviceContactCoordinator()
    private var completion: (([String: Any]?, Error?) -> Void)?

    private weak var presentedPicker: CNContactPickerViewController?

    func present(completion: @escaping ([String: Any]?, Error?) -> Void) {
        guard presentedPicker == nil else {
            BridgeDeviceSession.release(.contact)
            completion(nil, BridgeDeviceError.busy("contact"))
            return
        }
        guard let vc = BridgePresenter.topViewController() else {
            BridgeDeviceSession.release(.contact)
            completion(nil, BridgeDeviceError.noPresenter)
            return
        }
        self.completion = completion
        let picker = CNContactPickerViewController()
        picker.delegate = self
        presentedPicker = picker
        DispatchQueue.main.async { vc.present(picker, animated: true) }
    }

    func cancelIfNeeded() {
        presentedPicker?.dismiss(animated: true)
        presentedPicker = nil
    }

    func contactPickerDidCancel(_ picker: CNContactPickerViewController) {
        picker.dismiss(animated: true) { [weak self] in
            self?.presentedPicker = nil
            self?.finish(["cancelled": true], nil)
        }
    }

    func contactPicker(_ picker: CNContactPickerViewController, didSelect contact: CNContact) {
        picker.dismiss(animated: true) { [weak self] in
            guard let self else { return }
            self.presentedPicker = nil
        let phone = contact.phoneNumbers.first?.value.stringValue ?? ""
        let email = contact.emailAddresses.first?.value as String? ?? ""
        let name = CNContactFormatter.string(from: contact, style: .fullName) ?? ""
            self.finish([
                "name": name,
                "phone": phone,
                "email": email,
                "id": contact.identifier,
            ], nil)
        }
    }

    private func finish(_ result: [String: Any]?, _ error: Error?) {
        BridgeDeviceSession.release(.contact)
        let cb = completion
        completion = nil
        cb?(result, error)
    }
}
