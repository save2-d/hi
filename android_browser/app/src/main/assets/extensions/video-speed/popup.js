let sliderValue = document.getElementById("myRange");
const resetBtn = document.getElementById("resetbtn");
const logoImg = document.getElementById("logo-img");
const customSpeedContainer = document.getElementById("custom-speed-container");
const customSpeedInput = document.getElementById("custom-speed-input");
const customSpeedBtn = document.getElementById("custom-speed-btn");

document.querySelector("title").textContent = chrome.i18n.getMessage("extName");
resetBtn.innerHTML = chrome.i18n.getMessage("reset");

logoImg.addEventListener("click", () => {
  if (customSpeedContainer.style.display === "none") {
    customSpeedContainer.style.display = "flex";
  } else {
    customSpeedContainer.style.display = "none";
  }
});

customSpeedBtn.addEventListener("click", () => {
  let newSpeed = parseFloat(customSpeedInput.value);
  if (isNaN(newSpeed) || newSpeed < 0.10 || newSpeed > 20.00) {
    customSpeedInput.value = "";
    return;
  }

  if (newSpeed > parseFloat(sliderValue.getAttribute('max'))) {
    sliderValue.setAttribute('max', newSpeed.toString());
  }

  sliderValue.value = newSpeed.toString();
  setValue();
  updateSpeed(newSpeed.toString());
  customSpeedInput.value = "";
  customSpeedContainer.style.display = "none";
});

window.onload = function () {
  chrome.storage.sync.get(["key"], function (result) {
    if (result.key) {
      const savedSpeed = parseFloat(result.key);
      const defaultMax = 5.35;

      if (savedSpeed > defaultMax) {
        sliderValue.setAttribute('max', savedSpeed.toString());
      } else {
        sliderValue.setAttribute('max', defaultMax.toString());
      }
      sliderValue.value = savedSpeed.toString();
    }
    setValue();

    const params = {
      active: true,
      currentWindow: true,
    };

    chrome.tabs.query(params, (tabs) => {
      if (tabs && tabs.length > 0) {
        chrome.tabs.sendMessage(tabs[0].id, result.key || "1.0", function () {
          if (chrome.runtime.lastError) {
            // console.log(chrome.runtime.lastError.message);
          }
        });
      }
    });
  });
};

resetBtn.addEventListener("click", function () {
  sliderValue.setAttribute('max', '5.35');
  const params = {
    active: true,
    currentWindow: true,
  };

  sliderValue.value = "1";
  setValue();

  chrome.storage.sync.set({ key: "1" });
  chrome.tabs.query(params, (tabs) => {
    if (tabs && tabs.length > 0) {
      chrome.tabs.sendMessage(tabs[0].id, "1", function () {
        if (chrome.runtime.lastError) {
          // console.log(chrome.runtime.lastError.message);
        }
      });
    }
  });
});

sliderValue.addEventListener("change", () => {
  sliderValue.setAttribute('max', '5.35');
  const params = {
    active: true,
    currentWindow: true,
  };

  var storeValue = sliderValue.value;
  chrome.storage.sync.set({ key: storeValue });

  chrome.tabs.query(params, (tabs) => {
    if (tabs && tabs.length > 0) {
      chrome.tabs.sendMessage(tabs[0].id, storeValue, function () {
        if (chrome.runtime.lastError) {
          // console.log(chrome.runtime.lastError.message);
        }
      });
    }
  });
});

document.getElementById("minus").addEventListener("click", decreaseSpeed);
document.getElementById("plus").addEventListener("click", increaseSpeed);

document.querySelectorAll(".preset-btn").forEach((button) => {
  button.addEventListener("click", function () {
    sliderValue.setAttribute('max', '5.35');
    const speed = this.getAttribute("data-speed");
    sliderValue.value = speed;
    setValue();
    updateSpeed(speed);
  });
});

function decreaseSpeed() {
  let newSpeed = Number(sliderValue.value) - 0.25;
  if (newSpeed < 0.10) {
    newSpeed = 0.10;
  }
  sliderValue.value = newSpeed.toString();
  setValue();
  updateSpeed(newSpeed.toString());
}

function increaseSpeed() {
  let newSpeed = Number(sliderValue.value) + 0.25;
  if (newSpeed > 20.01) {
    newSpeed = 20.01;
  }
  if (newSpeed > parseFloat(sliderValue.getAttribute('max'))) {
    sliderValue.setAttribute('max', newSpeed.toString());
  }
  sliderValue.value = newSpeed.toString();
  setValue();
  updateSpeed(newSpeed.toString());
}

function updateSpeed(speed) {
  const params = {
    active: true,
    currentWindow: true,
  };
  chrome.storage.sync.set({ key: speed });
  chrome.tabs.query(params, (tabs) => {
    if (tabs && tabs.length > 0) {
      chrome.tabs.sendMessage(tabs[0].id, speed, function () {
        if (chrome.runtime.lastError) {
          // console.log(chrome.runtime.lastError.message);
        }
      });
    }
  });
}

const rangeV = document.getElementById('rangeV'),
  setValue = () => {
    const
      newValue = Number((sliderValue.value - sliderValue.min) * 100 / (sliderValue.getAttribute('max') - sliderValue.min)),
      newPosition = 10 - (newValue * 0.2);

    rangeV.innerHTML = `<span>${sliderValue.value + "x"}</span>`;
    rangeV.style.left = `calc(${newValue}% + (${newPosition}px))`;
  };

document.addEventListener("DOMContentLoaded", setValue);
sliderValue.addEventListener('input', setValue);