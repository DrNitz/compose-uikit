document.querySelectorAll("button.copy").forEach(function (btn) {
  btn.addEventListener("click", function () {
    var target = document.getElementById(btn.dataset.copy);
    if (!target) return;
    var text = target.innerText.replace(/\n+$/, "");
    var done = function () {
      var old = btn.textContent;
      btn.textContent = "Copied";
      setTimeout(function () { btn.textContent = old; }, 1400);
    };
    if (navigator.clipboard && navigator.clipboard.writeText) {
      navigator.clipboard.writeText(text).then(done, done);
    } else {
      var ta = document.createElement("textarea");
      ta.value = text;
      document.body.appendChild(ta);
      ta.select();
      document.execCommand("copy");
      document.body.removeChild(ta);
      done();
    }
  });
});
