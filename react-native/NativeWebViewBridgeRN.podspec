require 'json'

package = JSON.parse(File.read(File.join(__dir__, 'package.json')))

Pod::Spec.new do |s|
  s.name         = 'NativeWebViewBridgeRN'
  s.version      = package['version']
  s.summary      = 'React Native bridge for native-webview-bridge'
  s.license      = package['license']
  s.authors      = package['author']
  s.homepage     = package['homepage']
  s.platforms    = { :ios => '13.0' }
  s.source       = { :git => package['repository']['url'], :tag => s.version.to_s }

  s.source_files = 'ios/**/*.{h,m,mm,swift}',
                   '../ios/Sources/NativeWebViewBridge/**/*.swift'

  s.dependency 'React-Core'
end
