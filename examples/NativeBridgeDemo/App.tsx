import React, {useCallback, useEffect, useRef, useState} from 'react';
import {
  Platform,
  SafeAreaView,
  ScrollView,
  StatusBar,
  StyleSheet,
  Text,
  TextInput,
  TouchableOpacity,
  View,
} from 'react-native';
import {
  BridgeWebView,
  NativeBridge,
} from 'native-webview-bridge-react-native';

const DEFAULT_URL =
  Platform.OS === 'android'
    ? 'http://10.0.2.2:3001/playground'
    : 'http://localhost:3001/playground';

function App(): React.JSX.Element {
  const [url, setUrl] = useState(DEFAULT_URL);
  const [loadUrl, setLoadUrl] = useState(DEFAULT_URL);
  const [logs, setLogs] = useState<string[]>([]);
  const scrollRef = useRef<ScrollView>(null);

  const appendLog = useCallback((line: string) => {
    const stamp = new Date().toLocaleTimeString('en-GB', {hour12: false});
    setLogs(prev => [...prev.slice(-80), `${stamp}  ${line}`]);
    setTimeout(() => scrollRef.current?.scrollToEnd({animated: true}), 50);
  }, []);

  useEffect(() => {
    NativeBridge.start();
    NativeBridge.putData('demoApp', 'react-native');
    NativeBridge.putData('authToken', 'demo-token-rn');

    const unsubLoaded = NativeBridge.onWebViewLoaded(payload => {
      appendLog(`WEBVIEW_LOADED [${payload.phase}] ${payload.url}`);
    });

    const unsubWeb = NativeBridge.onWebEvent((event, payload) => {
      if (event === 'WEBVIEW_LOADED') return;
      appendLog(`send: ${event} → ${JSON.stringify(payload)}`);
    });

    appendLog('NativeBridge ready');
    return () => {
      unsubLoaded();
      unsubWeb();
    };
  }, [appendLog]);

  return (
    <SafeAreaView style={styles.root}>
      <StatusBar barStyle="light-content" backgroundColor="#0f172a" />
      <View style={styles.toolbar}>
        <TextInput
          style={styles.urlInput}
          value={url}
          onChangeText={setUrl}
          autoCapitalize="none"
          autoCorrect={false}
          keyboardType="url"
          placeholder="Playground URL"
          placeholderTextColor="#64748b"
        />
        <TouchableOpacity
          style={styles.loadBtn}
          onPress={() => {
            appendLog(`Loading ${url}`);
            setLoadUrl(url);
          }}>
          <Text style={styles.loadBtnText}>Load</Text>
        </TouchableOpacity>
      </View>

      <BridgeWebView
        source={{uri: loadUrl}}
        style={styles.webview}
        mediaCapture
      />

      <Text style={styles.logTitle}>Native event log</Text>
      <ScrollView ref={scrollRef} style={styles.logBox}>
        {logs.map((line, i) => (
          <Text key={i} style={styles.logLine}>
            {line}
          </Text>
        ))}
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  root: {
    flex: 1,
    backgroundColor: '#0f172a',
  },
  toolbar: {
    flexDirection: 'row',
    alignItems: 'center',
    padding: 8,
    gap: 8,
  },
  urlInput: {
    flex: 1,
    borderWidth: 1,
    borderColor: '#334155',
    borderRadius: 8,
    paddingHorizontal: 12,
    paddingVertical: 8,
    color: '#e2e8f0',
    fontSize: 13,
  },
  loadBtn: {
    backgroundColor: '#0ea5e9',
    paddingHorizontal: 16,
    paddingVertical: 10,
    borderRadius: 8,
  },
  loadBtnText: {
    color: '#0f172a',
    fontWeight: '700',
    fontSize: 13,
  },
  webview: {
    flex: 1,
  },
  logTitle: {
    color: '#94a3b8',
    fontSize: 12,
    fontWeight: '600',
    paddingHorizontal: 12,
    paddingTop: 8,
  },
  logBox: {
    maxHeight: 120,
    backgroundColor: '#1e293b',
    padding: 12,
  },
  logLine: {
    fontFamily: Platform.OS === 'ios' ? 'Menlo' : 'monospace',
    fontSize: 11,
    color: '#86efac',
    marginBottom: 4,
  },
});

export default App;
