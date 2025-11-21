let currentSpeed = 1.0;

const setSpeed = (speed) => {
  currentSpeed = speed;
  document.querySelectorAll("video").forEach(video => {
    video.playbackRate = speed;
  });
};

chrome.storage.sync.get(["key"], function (result) {
  if (result.key) {
    setSpeed(parseFloat(result.key));
  }
});

chrome.runtime.onMessage.addListener((playbackRate) => {
  setSpeed(parseFloat(playbackRate));
});

const observer = new MutationObserver((mutations) => {
  mutations.forEach((mutation) => {
    mutation.addedNodes.forEach((node) => {
      if (node.tagName === 'VIDEO') {
        node.playbackRate = currentSpeed;
      }
      if (node.querySelectorAll) {
        node.querySelectorAll('video').forEach(video => {
          video.playbackRate = currentSpeed;
        });
      }
    });
  });
});

function startObserver() {
  if (!document.body) {
    // Retry shortly if body not yet available
    return setTimeout(startObserver, 100);
  }

  observer.observe(document.body, {
    childList: true,
    subtree: true,
  });
}

startObserver();

