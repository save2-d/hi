chrome.runtime.onInstalled.addListener(function (details) {
  chrome.storage.sync.set({ key: "1" });
});

chrome.tabs.onUpdated.addListener(function (tabId, changeInfo, tab) {
  const internalPagePatterns = [
    /^chrome:\/\//,
    /^chrome-extension:\/\//
  ];
  if (!tab.url || internalPagePatterns.some(pattern => pattern.test(tab.url))) {
    return; // Ignore internal pages
  }

  chrome.storage.sync.get(["key"], function (result) {
    chrome.tabs.query({ active: true, currentWindow: true }, (tabs) => {
      if (tabs && tabs.length > 0) {
        chrome.tabs.sendMessage(tabs[0].id, result.key, function () {
          if (chrome.runtime.lastError) {
            // console.log(chrome.runtime.lastError.message);
          }
        });
      }
    });
  });
});

chrome.commands.onCommand.addListener(function (command) {
  switch (command) {
    case "left":
      chrome.storage.sync.get(["key"], function (result) {
        let newSpeed = Number(result.key) - 0.25;
        if (newSpeed < 0.01) {
          newSpeed = 0.01;
        }
        chrome.storage.sync.set({ key: newSpeed.toString() });
        chrome.tabs.query({ active: true, currentWindow: true }, (tabs) => {
          if (tabs && tabs.length > 0) {
            chrome.tabs.sendMessage(
              tabs[0].id,
              newSpeed.toString(),
              function () {
                if (chrome.runtime.lastError) {
                  // console.log(chrome.runtime.lastError.message);
                }
              }
            );
          }
        });
      });
      break;

    case "right":
      chrome.storage.sync.get(["key"], function (result) {
        let newSpeed = Number(result.key) + 0.25;
        if (newSpeed > 6.0) {
          newSpeed = 6.0;
        }
        chrome.storage.sync.set({ key: newSpeed.toString() });
        chrome.tabs.query({ active: true, currentWindow: true }, (tabs) => {
          if (tabs && tabs.length > 0) {
            chrome.tabs.sendMessage(
              tabs[0].id,
              newSpeed.toString(),
              function () {
                if (chrome.runtime.lastError) {
                  // console.log(chrome.runtime.lastError.message);
                }
              }
            );
          }
        });
      });
      break;
  }
});
