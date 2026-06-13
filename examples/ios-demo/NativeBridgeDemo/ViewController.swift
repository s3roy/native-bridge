import UIKit
import NativeWebViewBridge

final class ViewController: UIViewController {
    private let webView = BridgeWebView()
    private let urlField = UITextField()
    private let loadButton = UIButton(type: .system)
    private let logView = UITextView()

    private let defaults = UserDefaults.standard
    private let urlKey = "playground_url"

    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = UIColor(red: 0.06, green: 0.09, blue: 0.16, alpha: 1)
        setupChrome()
        wireBridge()
        loadSavedUrl()
    }

    private func setupChrome() {
        urlField.translatesAutoresizingMaskIntoConstraints = false
        urlField.borderStyle = .roundedRect
        urlField.keyboardType = .URL
        urlField.autocapitalizationType = .none
        urlField.autocorrectionType = .no
        urlField.placeholder = "Playground URL"
        urlField.textColor = .white
        urlField.backgroundColor = UIColor(white: 0.15, alpha: 1)

        loadButton.translatesAutoresizingMaskIntoConstraints = false
        loadButton.setTitle("Load", for: .normal)
        loadButton.addTarget(self, action: #selector(loadTapped), for: .touchUpInside)

        webView.translatesAutoresizingMaskIntoConstraints = false

        logView.translatesAutoresizingMaskIntoConstraints = false
        logView.isEditable = false
        logView.font = UIFont.monospacedSystemFont(ofSize: 11, weight: .regular)
        logView.textColor = UIColor(red: 0.53, green: 0.94, blue: 0.67, alpha: 1)
        logView.backgroundColor = UIColor(red: 0.12, green: 0.16, blue: 0.23, alpha: 1)

        view.addSubview(urlField)
        view.addSubview(loadButton)
        view.addSubview(webView)
        view.addSubview(logView)

        let guide = view.safeAreaLayoutGuide
        NSLayoutConstraint.activate([
            urlField.topAnchor.constraint(equalTo: guide.topAnchor, constant: 8),
            urlField.leadingAnchor.constraint(equalTo: guide.leadingAnchor, constant: 12),
            loadButton.centerYAnchor.constraint(equalTo: urlField.centerYAnchor),
            loadButton.leadingAnchor.constraint(equalTo: urlField.trailingAnchor, constant: 8),
            loadButton.trailingAnchor.constraint(equalTo: guide.trailingAnchor, constant: -12),
            loadButton.widthAnchor.constraint(equalToConstant: 64),

            webView.topAnchor.constraint(equalTo: urlField.bottomAnchor, constant: 8),
            webView.leadingAnchor.constraint(equalTo: guide.leadingAnchor),
            webView.trailingAnchor.constraint(equalTo: guide.trailingAnchor),

            logView.topAnchor.constraint(equalTo: webView.bottomAnchor, constant: 8),
            logView.leadingAnchor.constraint(equalTo: guide.leadingAnchor),
            logView.trailingAnchor.constraint(equalTo: guide.trailingAnchor),
            logView.bottomAnchor.constraint(equalTo: guide.bottomAnchor),
            logView.heightAnchor.constraint(equalToConstant: 120),
        ])
    }

    private func wireBridge() {
        NativeBridge.shared.putData("demoApp", "ios")
        NativeBridge.shared.putData("authToken", "demo-token-ios")

        NativeBridge.shared.setWebViewLoadedHandler { [weak self] payload, webViewId, _ in
            self?.appendLog(
                "WEBVIEW_LOADED [\(payload.phase.rawValue)] id=\(webViewId)\n" +
                    "  url=\(payload.url)"
            )
        }

        NativeBridge.shared.setWebEventHandler { [weak self] event, payload, webViewId, _ in
            guard event != WebEvents.webViewLoaded else { return }
            self?.appendLog("send: \(event) (wv=\(webViewId))")
        }

        webView.setOnWebViewLoaded { [weak self] payload in
            self?.appendLog("per-WebView: \(payload.phase.rawValue) \(payload.url)")
        }
    }

    private func loadSavedUrl() {
        let defaultUrl = "http://localhost:3001/playground"
        urlField.text = defaults.string(forKey: urlKey) ?? defaultUrl
        loadTapped()
    }

    @objc private func loadTapped() {
        guard let text = urlField.text?.trimmingCharacters(in: .whitespacesAndNewlines),
              let url = URL(string: text) else { return }
        defaults.set(text, forKey: urlKey)
        appendLog("Loading \(text)")
        webView.load(URLRequest(url: url))
    }

    private func appendLog(_ line: String) {
        let formatter = DateFormatter()
        formatter.dateFormat = "HH:mm:ss"
        let stamp = formatter.string(from: Date())
        DispatchQueue.main.async {
            self.logView.text += "\(stamp)  \(line)\n"
            let end = NSMakeRange(self.logView.text.count - 1, 1)
            self.logView.scrollRangeToVisible(end)
        }
    }
}
