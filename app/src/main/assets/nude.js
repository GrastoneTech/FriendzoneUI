(function () {
	var a = (function () {
		var e = null,
			c = null,
			h = null,
			d = function () {
				e = document.createElement("canvas");
				e.style.display = "none";
				var j = document.getElementsByTagName("body")[0];
				j.appendChild(e);
				c = e.getContext("2d")
			},
			b = function (k) {
				var j = document.getElementById(k);
				e.width = j.width;
				e.height = j.height;
				h = null;
				c.drawImage(j, 0, 0)
			},
			g = function (j) {
				e.width = j.width;
				e.height = j.height;
				h = null;
				c.drawImage(j, 0, 0)
			},
			i = function () {
				var l = c.getImageData(0, 0, e.width, e.height),
					m = l.data;
				var j = new Worker("worker.nude.js"),
					k = [m, e.width, e.height];
				j.postMessage(k);
				j.onmessage = function (n) {
					f(n.data)
				}
			},
			f = function (j) {
				if (h) {
					h(j)
				} else {
					if (j) {
						console.log("the picture contains nudity")
					}
				}
			};
		return {
			init: function () {
				d();
				if (!!!window.Worker) {
					document.write(unescape("%3Cscript src='noworker.nude.js' type='text/javascript'%3E%3C/script%3E"))
				}
			},
			load: function (j) {
				if (typeof (j) == "string") {
					b(j)
				} else {
					g(j)
				}
			},
			scan: function (j) {
				if (arguments.length > 0 && typeof (arguments[0]) == "function") {
					h = j
				}
				i()
			}
		}
	})();
	if (!window.nude) {
		window.nude = a
	}
	a.init()
})();