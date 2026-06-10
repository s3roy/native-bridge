import Foundation

enum BridgeDeviceSession {

    enum Resource: String {
        case camera, gallery, contact, location, share
    }

    private static let lock = NSLock()
    private static var busy = Set<Resource>()
    private static var locationCancel: (() -> Void)?

    static func tryOccupy(_ resource: Resource) -> Bool {
        lock.lock(); defer { lock.unlock() }
        if busy.contains(resource) { return false }
        busy.insert(resource)
        return true
    }

    static func release(_ resource: Resource) {
        lock.lock(); defer { lock.unlock() }
        busy.remove(resource)
    }

    static func isBusy(_ resource: Resource) -> Bool {
        lock.lock(); defer { lock.unlock() }
        return busy.contains(resource)
    }

    static func releaseAll() {
        lock.lock()
        locationCancel?()
        locationCancel = nil
        busy.removeAll()
        lock.unlock()
        BridgeDevice.releaseTorch()
    }

    static func cancel(_ resource: Resource) {
        lock.lock()
        if resource == .location {
            locationCancel?()
            locationCancel = nil
        }
        busy.remove(resource)
        lock.unlock()
        if resource == .camera { BridgeDevice.releaseTorch() }
    }

    static func registerLocationCancel(_ cancel: @escaping () -> Void) {
        lock.lock(); defer { lock.unlock() }
        locationCancel = cancel
    }

    static func clearLocationCancel() {
        lock.lock(); defer { lock.unlock() }
        locationCancel = nil
    }

    static func status() -> [String: Any] {
        lock.lock(); defer { lock.unlock() }
        return [
            "camera": busy.contains(.camera) ? "busy" : "idle",
            "gallery": busy.contains(.gallery) ? "busy" : "idle",
            "contact": busy.contains(.contact) ? "busy" : "idle",
            "location": busy.contains(.location) ? "busy" : "idle",
        ]
    }
}
