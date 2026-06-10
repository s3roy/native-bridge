import Foundation

/// Auto-captures URLSession traffic app-wide with no host-app code, by
/// registering a URLProtocol and injecting it into the default session config.
final class BridgeURLProtocol: URLProtocol, URLSessionDataDelegate {

    private static let handledKey = "BridgeURLProtocolHandled"
    private var session: URLSession?
    private var dataTask: URLSessionDataTask?
    private var responseData = Data()
    private var httpResponse: HTTPURLResponse?
    private var startTime = Date()

    static func enable() {
        URLProtocol.registerClass(BridgeURLProtocol.self)
        swizzleDefaultSessionConfiguration()
    }

    // MARK: URLProtocol

    override class func canInit(with request: URLRequest) -> Bool {
        if URLProtocol.property(forKey: handledKey, in: request) != nil { return false }
        guard let scheme = request.url?.scheme?.lowercased() else { return false }
        return scheme == "http" || scheme == "https"
    }

    override class func canonicalRequest(for request: URLRequest) -> URLRequest { request }

    override func startLoading() {
        startTime = Date()
        guard let mutable = (request as NSURLRequest).mutableCopy() as? NSMutableURLRequest else { return }
        URLProtocol.setProperty(true, forKey: BridgeURLProtocol.handledKey, in: mutable)

        let config = URLSessionConfiguration.default
        session = URLSession(configuration: config, delegate: self, delegateQueue: nil)
        dataTask = session?.dataTask(with: mutable as URLRequest)
        dataTask?.resume()
    }

    override func stopLoading() {
        dataTask?.cancel()
        session?.invalidateAndCancel()
    }

    // MARK: URLSessionDataDelegate

    func urlSession(_ session: URLSession, dataTask: URLSessionDataTask,
                    didReceive response: URLResponse,
                    completionHandler: @escaping (URLSession.ResponseDisposition) -> Void) {
        httpResponse = response as? HTTPURLResponse
        client?.urlProtocol(self, didReceive: response, cacheStoragePolicy: .notAllowed)
        completionHandler(.allow)
    }

    func urlSession(_ session: URLSession, dataTask: URLSessionDataTask, didReceive data: Data) {
        responseData.append(data)
        client?.urlProtocol(self, didLoad: data)
    }

    func urlSession(_ session: URLSession, task: URLSessionTask, didCompleteWithError error: Error?) {
        if let error = error {
            record(error: error.localizedDescription)
            client?.urlProtocol(self, didFailWithError: error)
        } else {
            record(error: nil)
            client?.urlProtocolDidFinishLoading(self)
        }
        self.session?.finishTasksAndInvalidate()
    }

    // MARK: capture

    private func record(error: String?) {
        let duration = Int(Date().timeIntervalSince(startTime) * 1000)
        let reqHeaders = request.allHTTPHeaderFields ?? [:]
        let reqBody = request.httpBody.flatMap { String(data: $0, encoding: .utf8) }
        let respHeaders = (httpResponse?.allHeaderFields as? [String: String]) ?? [:]
        let bodyText = String(data: responseData, encoding: .utf8)

        let call = ApiCall(
            id: UUID().uuidString,
            method: request.httpMethod ?? "GET",
            url: request.url?.absoluteString ?? "",
            requestHeaders: reqHeaders,
            requestBody: reqBody,
            status: httpResponse?.statusCode ?? -1,
            responseHeaders: respHeaders,
            responseBody: bodyText,
            durationMs: duration,
            timestamp: Int(startTime.timeIntervalSince1970 * 1000),
            error: error
        )
        NativeBridge.shared.recordApiCall(call)
    }

    // MARK: swizzle so URLSession(configuration: .default) includes us

    private static func swizzleDefaultSessionConfiguration() {
        let cls: AnyClass = URLSessionConfiguration.self
        guard
            let original = class_getClassMethod(cls, #selector(getter: URLSessionConfiguration.default)),
            let swizzled = class_getClassMethod(cls, #selector(URLSessionConfiguration.bridge_default))
        else { return }
        method_exchangeImplementations(original, swizzled)
    }
}

extension URLSessionConfiguration {
    @objc class func bridge_default() -> URLSessionConfiguration {
        // After swizzling this calls the ORIGINAL implementation.
        let config = bridge_default()
        var protocols = config.protocolClasses ?? []
        protocols.insert(BridgeURLProtocol.self, at: 0)
        config.protocolClasses = protocols
        return config
    }
}
