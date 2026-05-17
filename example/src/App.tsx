import { useEffect } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import {
  initializeCrashScreenshot,
  NativeCrashScreenshot,
} from 'react-native-crash-screenshot';

export default function App() {
  useEffect(() => {
    initializeCrashScreenshot();
  }, []);

  return (
    <View style={styles.container}>
      <Pressable
        style={styles.button}
        onPress={() => {
          throw new Error('react-native-crash-screenshot: test JS crash');
        }}>
        <Text style={styles.label}>Trigger JS crash</Text>
      </Pressable>
      <Pressable
        style={styles.button}
        onPress={() => {
          NativeCrashScreenshot.triggerTestNativeCrash();
        }}>
        <Text style={styles.label}>Trigger native crash</Text>
      </Pressable>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    gap: 16,
  },
  button: {
    paddingHorizontal: 20,
    paddingVertical: 12,
    backgroundColor: '#2563eb',
    borderRadius: 8,
  },
  label: {
    color: '#fff',
    fontWeight: '600',
  },
});
