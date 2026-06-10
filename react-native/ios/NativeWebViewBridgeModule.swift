import Foundation
import React

@objc(NativeWebViewBridge)
class NativeWebViewBridgeModule: RCTEventEmitter {

    private static weak var shared: NativeWebViewBridgeModule?
    private static var emitRelayInstalled = false

    override init() {
        super.init()
        NativeWebViewBridgeModule.shared = self
        Self.installEmitRelayIfNeeded()
    }

    @objc override static func requiresMainQueueSetup() -> Bool { true }

    override func supportedEvents() -> [String]! {
        [Self.eventName]
    }

    @objc override func addListener(_ eventName: String!) {}

    @objc override func removeListeners(_ count: Double) {}

    @objc func start(_ resolve: RCTPromiseResolveBlock, rejecter reject: RCTPromiseRejectBlock) {
        NativeBridge.start()
        Self.installEmitRelayIfNeeded()
        resolve(nil)
    }

    @objc func putData(
        _ key: String,
        value: String,
        resolver resolve: RCTPromiseResolveBlock,
        rejecter reject: RCTPromiseRejectBlock
    ) {
        NativeBridge.shared.putData(key, value)
        resolve(nil)
    }

    @objc func publishEvent(
        _ event: String,
        params: NSDictionary?,
        resolver resolve: RCTPromiseResolveBlock,
        rejecter reject: RCTPromiseRejectBlock
    ) {
        let payload = params ?? [:]
        NativeBridge.shared.publishEvent(event, payload)
        resolve(nil)
    }

    @objc func dispatch(
        _ method: String,
        params: NSDictionary?,
        resolver resolve: @escaping RCTPromiseResolveBlock,
        rejecter reject: @escaping RCTPromiseRejectBlock
    ) {
        let p = params as? [String: Any] ?? [:]
        NativeBridge.shared.dispatchBridge(method: method, params: p) { result, error in
            if let error = error {
                reject("BRIDGE_ERROR", error.localizedDescription, error)
                return
            }
            resolve(result)
        }
    }

    private static let eventName = "NativeBridgeEvent"

    private static func installEmitRelayIfNeeded() {
        guard !emitRelayInstalled else { return }
        emitRelayInstalled = true
        NativeBridge.shared.addEmitListener { event, payload in
            DispatchQueue.main.async {
                shared?.sendEvent(
                    withName: eventName,
                    body: ["event": event, "payload": payload]
                )
            }
        }
    }
}
