import Foundation
import AVFoundation
import CoreLocation
import Photos
import UserNotifications
import UIKit

struct PermissionResult {
    let permission: String
    let status: String
    let canAskAgain: Bool

    func toDictionary() -> [String: Any] {
        ["permission": permission, "status": status, "canAskAgain": canAskAgain]
    }
}

enum BridgePermissionNames {
    static let camera = "camera"
    static let microphone = "microphone"
    static let location = "location"
    static let locationCoarse = "locationCoarse"
    static let notifications = "notifications"
    static let photos = "photos"
    static let bluetooth = "bluetooth"
    static let contacts = "contacts"

    static let all = [
        camera, microphone, location, locationCoarse,
        notifications, photos, bluetooth, contacts,
    ]
}

enum BridgePermissions {

    private static var locationManager: CLLocationManager?
    private static var locationCompletion: ((PermissionResult) -> Void)?
    private static var locationPermissionName = BridgePermissionNames.location

    // MARK: - Status

    static func getStatus(_ name: String, completion: @escaping (PermissionResult) -> Void) {
        switch name {
        case BridgePermissionNames.camera:
            let s = AVCaptureDevice.authorizationStatus(for: .video)
            completion(mapAvStatus(name, s))
        case BridgePermissionNames.microphone:
            switch AVAudioSession.sharedInstance().recordPermission {
            case .granted: completion(PermissionResult(permission: name, status: "granted", canAskAgain: false))
            case .denied: completion(PermissionResult(permission: name, status: "blocked", canAskAgain: false))
            case .undetermined: completion(PermissionResult(permission: name, status: "denied", canAskAgain: true))
            @unknown default: completion(PermissionResult(permission: name, status: "unavailable", canAskAgain: false))
            }
        case BridgePermissionNames.location, BridgePermissionNames.locationCoarse:
            let s = CLLocationManager.authorizationStatus()
            completion(mapLocationStatus(name, s))
        case BridgePermissionNames.photos:
            let s = PHPhotoLibrary.authorizationStatus()
            completion(mapPhotoStatus(name, s))
        case BridgePermissionNames.notifications:
            UNUserNotificationCenter.current().getNotificationSettings { settings in
                DispatchQueue.main.async {
                    let status: String
                    switch settings.authorizationStatus {
                    case .authorized, .provisional, .ephemeral: status = "granted"
                    case .denied: status = "blocked"
                    case .notDetermined: status = "denied"
                    @unknown default: status = "unavailable"
                    }
                    completion(PermissionResult(
                        permission: name,
                        status: status,
                        canAskAgain: status == "denied"
                    ))
                }
            }
        case BridgePermissionNames.bluetooth:
            completion(PermissionResult(permission: name, status: "granted", canAskAgain: false))
        case BridgePermissionNames.contacts:
            completion(PermissionResult(permission: name, status: "unavailable", canAskAgain: false))
        default:
            completion(PermissionResult(permission: name, status: "unavailable", canAskAgain: false))
        }
    }

    static func getAllStatuses(completion: @escaping ([[String: Any]]) -> Void) {
        let group = DispatchGroup()
        var results: [[String: Any]] = []
        let lock = NSLock()
        for name in BridgePermissionNames.all {
            group.enter()
            getStatus(name) { result in
                lock.lock()
                results.append(result.toDictionary())
                lock.unlock()
                group.leave()
            }
        }
        group.notify(queue: .main) { completion(results) }
    }

    // MARK: - Request

    static func request(_ name: String, completion: @escaping (PermissionResult) -> Void) {
        switch name {
        case BridgePermissionNames.camera:
            AVCaptureDevice.requestAccess(for: .video) { granted in
                let result = PermissionResult(
                    permission: name,
                    status: granted ? "granted" : "denied",
                    canAskAgain: !granted
                )
                finish(result, completion: completion)
            }
        case BridgePermissionNames.microphone:
            AVAudioSession.sharedInstance().requestRecordPermission { granted in
                let result = PermissionResult(
                    permission: name,
                    status: granted ? "granted" : "denied",
                    canAskAgain: !granted
                )
                finish(result, completion: completion)
            }
        case BridgePermissionNames.location, BridgePermissionNames.locationCoarse:
            locationPermissionName = name
            locationCompletion = completion
            let mgr = CLLocationManager()
            locationManager = mgr
            mgr.delegate = LocationDelegate.shared
            mgr.requestWhenInUseAuthorization()
        case BridgePermissionNames.photos:
            PHPhotoLibrary.requestAuthorization { s in
                finish(mapPhotoStatus(name, s), completion: completion)
            }
        case BridgePermissionNames.notifications:
            UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound, .badge]) { granted, _ in
                let result = PermissionResult(
                    permission: name,
                    status: granted ? "granted" : "denied",
                    canAskAgain: !granted
                )
                finish(result, completion: completion)
            }
        default:
            getStatus(name, completion: completion)
        }
    }

    static func openAppSettings() -> Bool {
        guard let url = URL(string: UIApplication.openSettingsURLString) else { return false }
        DispatchQueue.main.async {
            UIApplication.shared.open(url)
        }
        return true
    }

    static func isGranted(_ name: String) -> Bool {
        switch name {
        case BridgePermissionNames.camera:
            return AVCaptureDevice.authorizationStatus(for: .video) == .authorized
        case BridgePermissionNames.microphone:
            return AVAudioSession.sharedInstance().recordPermission == .granted
        default:
            return false
        }
    }

    // MARK: - Helpers

    private static func finish(_ result: PermissionResult, completion: @escaping (PermissionResult) -> Void) {
        DispatchQueue.main.async {
            completion(result)
            NativeBridge.shared.emit("permission.change", result.toDictionary())
        }
    }

    static func handleLocationAuth(_ status: CLAuthorizationStatus) {
        guard let completion = locationCompletion else { return }
        locationCompletion = nil
        finish(mapLocationStatus(locationPermissionName, status), completion: completion)
    }

    private static func mapAvStatus(_ name: String, _ s: AVAuthorizationStatus) -> PermissionResult {
        switch s {
        case .authorized: return PermissionResult(permission: name, status: "granted", canAskAgain: false)
        case .denied: return PermissionResult(permission: name, status: "blocked", canAskAgain: false)
        case .restricted: return PermissionResult(permission: name, status: "unavailable", canAskAgain: false)
        case .notDetermined: return PermissionResult(permission: name, status: "denied", canAskAgain: true)
        @unknown default: return PermissionResult(permission: name, status: "unavailable", canAskAgain: false)
        }
    }

    private static func mapLocationStatus(_ name: String, _ s: CLAuthorizationStatus) -> PermissionResult {
        switch s {
        case .authorizedAlways, .authorizedWhenInUse:
            return PermissionResult(permission: name, status: "granted", canAskAgain: false)
        case .denied: return PermissionResult(permission: name, status: "blocked", canAskAgain: false)
        case .restricted: return PermissionResult(permission: name, status: "unavailable", canAskAgain: false)
        case .notDetermined: return PermissionResult(permission: name, status: "denied", canAskAgain: true)
        @unknown default: return PermissionResult(permission: name, status: "unavailable", canAskAgain: false)
        }
    }

    private static func mapPhotoStatus(_ name: String, _ s: PHAuthorizationStatus) -> PermissionResult {
        switch s {
        case .authorized, .limited: return PermissionResult(permission: name, status: "granted", canAskAgain: false)
        case .denied: return PermissionResult(permission: name, status: "blocked", canAskAgain: false)
        case .restricted: return PermissionResult(permission: name, status: "unavailable", canAskAgain: false)
        case .notDetermined: return PermissionResult(permission: name, status: "denied", canAskAgain: true)
        @unknown default: return PermissionResult(permission: name, status: "unavailable", canAskAgain: false)
        }
    }
}

private final class LocationDelegate: NSObject, CLLocationManagerDelegate {
    static let shared = LocationDelegate()
    func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
        BridgePermissions.handleLocationAuth(manager.authorizationStatus)
    }
}
