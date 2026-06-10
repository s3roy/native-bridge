module.exports = {
  dependency: {
    platforms: {
      android: {
        sourceDir: './android',
        packageImportPath: 'import com.bridge.rn.NativeWebViewBridgePackage;',
        packageInstance: 'new NativeWebViewBridgePackage()',
      },
      ios: {
        podspecPath: './NativeWebViewBridgeRN.podspec',
      },
    },
  },
};
