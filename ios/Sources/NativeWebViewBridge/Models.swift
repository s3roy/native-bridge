import Foundation

/// A single captured native API call (request + response).
public struct ApiCall: Codable {
    public let id: String
    public let method: String
    public let url: String
    public let requestHeaders: [String: String]
    public let requestBody: String?
    public let status: Int
    public let responseHeaders: [String: String]
    public let responseBody: String?
    public let durationMs: Int
    public let timestamp: Int
    public let error: String?

    func toDictionary() -> [String: Any] {
        return [
            "id": id,
            "method": method,
            "url": url,
            "requestHeaders": requestHeaders,
            "requestBody": requestBody as Any,
            "status": status,
            "responseHeaders": responseHeaders,
            "responseBody": responseBody as Any,
            "durationMs": durationMs,
            "timestamp": timestamp,
            "error": error as Any,
        ]
    }
}

/// A captured push or local notification.
public struct NotificationRecord: Codable {
    public let id: String
    public let title: String?
    public let body: String?
    public let data: [String: String]
    public let timestamp: Int

    func toDictionary() -> [String: Any] {
        return [
            "id": id,
            "title": title as Any,
            "body": body as Any,
            "data": data,
            "timestamp": timestamp,
        ]
    }
}

/// Thread-safe bounded ring buffer.
final class RingBuffer<T> {
    private var items: [T] = []
    private let max: Int
    private let lock = NSLock()

    init(max: Int) { self.max = max }

    func add(_ item: T) {
        lock.lock(); defer { lock.unlock() }
        items.append(item)
        if items.count > max { items.removeFirst(items.count - max) }
    }

    func all() -> [T] {
        lock.lock(); defer { lock.unlock() }
        return items
    }
}
