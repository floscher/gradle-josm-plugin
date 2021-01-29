// Configuration for Karma in CI
config.set({
  browsers: ['ChromiumHeadlessNoSandbox'],
  customLaunchers: {
    ChromiumHeadlessNoSandbox: {
      base: 'ChromiumHeadless',
      flags: ['--no-sandbox', '--enable-logging']
    }
  },
  client: { captureConsole: true }
});
