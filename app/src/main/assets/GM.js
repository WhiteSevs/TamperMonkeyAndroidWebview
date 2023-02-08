(function (GM) {
	if (typeof window.GM_INJECTION !== "undefined") {
		console.log("GM API 已注入过");
		return;
	}
	window.unsafeWindow = window;
	/* 设置回调对象 */
	GM.callback = {
		GM_xmlhttpRequest: {
			onload: {},
			onabort: {},
			onerror: {},
			ontimeout: {},
		},
		GM_xmlhttpRequest_iframe: {
			onload: {},
			onabort: {},
			onerror: {},
			ontimeout: {},
			iframeNode: {},
		},
		GM_menuCommand: {},
		GM_parentIframeMsg:
			() => {} /* iframe内调用top的函数，用于通知iframe内网址改变，如果iframe内离开的网址是跨域，那么失效 */,
	};
	GM.callback.GM_parentIframeMsg = (childIframeNode) => {
		console.log(
			"GM.callback.GM_parentIframeMsg 收到iframe网址改变消息",
			childIframeNode
		);
		let checkChildFrameLoad = setInterval(() => {
			if (childIframeNode.contentWindow == null) {
				console.log("GM.callback.GM_parentIframeMsg 子frame处于跨域，不可操作");
				clearInterval(checkChildFrameLoad);
			} else if (childIframeNode.contentWindow.BEFOREUNLOADSTATUS) {
				console.log("GM.callback.GM_parentIframeMsg 仍处于网页离开状态");
				return;
			} else {
				if (
					typeof childIframeNode.contentDocument.body != "undefined" &&
					typeof childIframeNode.contentDocument.body.append != "undefined" &&
					typeof childIframeNode.contentDocument.querySelector != "undefined"
				) {
					clearInterval(checkChildFrameLoad);
					console.log("GM.callback.GM_parentIframeMsg iframe内部注入js");
					let GMIframeApiNode = document.createElement("script"); /* 油猴api */
					let GMIframeScriptsNode =
						document.createElement("script"); /* 油猴iframe内执行的脚本 */
					let GMURLChangeNode =
						document.createElement("script"); /* 监听网址改变 */
					GMIframeApiNode.setAttribute("type", "text/javascript");
					GMIframeScriptsNode.setAttribute("type", "text/javascript");
					GMURLChangeNode.setAttribute("type", "text/javascript");

					GMIframeApiNode.innerHTML = GM.getGMIframeApi();
					GMIframeScriptsNode.innerHTML = GM.getIframeScriptText();
					GMURLChangeNode.innerHTML = `
							window.onbeforeunload = ()=>{
								console.log("GM.callback.GM_parentIframeMsg 网址改变");
								top.window._GM_.callback.GM_parentIframeMsg(self.frameElement);
							}
						`;
					setTimeout(() => {
						childIframeNode.contentDocument.body.append(GMIframeApiNode);
						childIframeNode.contentDocument.body.append(GMIframeScriptsNode);
						childIframeNode.contentDocument.body.append(GMURLChangeNode);
						console.log(
							"GM.callback.GM_parentIframeMsg 注入油猴api的js到iframe内",
							GMIframeApiNode
						);
						console.log(
							"GM.callback.GM_parentIframeMsg 注入js到iframe内",
							childIframeNode
						);
					}, 500);
				}
			}
		}, 100);
	};
	GM.injectionScript = function (iframeNode) {
		/* iframe内部注入js */
		console.log("GM.injectionScript iframe内部注入js", iframeNode);
		let iframeInterval = setInterval(() => {
			if (typeof iframeNode.contentWindow === "undefined") {
				console.log("GM.injectionScript 跨域，不可操作");
				clearInterval(iframeInterval);
			}
			if (
				typeof iframeNode.contentDocument.body != "undefined" &&
				typeof iframeNode.contentDocument.body.append != "undefined" &&
				typeof iframeNode.contentDocument.querySelector != "undefined"
			) {
				clearInterval(iframeInterval);
				let GMIframeApiNode = document.createElement("script"); /* 油猴api */
				let GMIframeScriptsNode =
					document.createElement("script"); /* 油猴iframe内执行的脚本 */
				let GMListenReloadNode =
					document.createElement("script"); /* 监听重载 */
				GMIframeApiNode.setAttribute("type", "text/javascript");
				GMIframeScriptsNode.setAttribute("type", "text/javascript");
				GMListenReloadNode.setAttribute("type", "text/javascript");

				GMIframeApiNode.innerHTML = GM.getGMIframeApi();
				GMIframeScriptsNode.innerHTML = GM.getIframeScriptText();
				GMListenReloadNode.innerHTML = `
				window.onbeforeunload = ()=>{
					self.BEFOREUNLOADSTATUS=true;
					console.log("GM.injectionScript iframe内网页重载",self.frameElement);
					if(self.frameElement == null){
						console.log("GM.injectionScript 子frame为空");
					}else{
						top.window._GM_.callback.GM_parentIframeMsg(self.frameElement);
					}
					
				}
				`;
				setTimeout(() => {
					iframeNode.contentDocument.body.append(GMIframeApiNode);
					iframeNode.contentDocument.body.append(GMIframeScriptsNode);
					iframeNode.contentDocument.body.append(GMListenReloadNode);
					console.log(
						"GM.injectionScript 注入油猴api的js到iframe内",
						GMIframeApiNode
					);
					console.log(
						"GM.injectionScript 注入油猴脚本的js到iframe内",
						GMIframeScriptsNode
					);
					console.log("GM.injectionScript 注入js到iframe内", iframeNode);
				}, 500);
			}
		}, 100);
	};
	GM.getGUID = function () {
		/* 获取唯一性的UID */
		return "xxxxxxxx_xxxx_4xxx_yxxx_xxxxxxxxxxxx".replace(
			/[xy]/g,
			function (c) {
				let r = (Math.random() * 16) | 0,
					v = c == "x" ? r : (r & 0x3) | 0x8;
				return v.toString(16);
			}
		);
	};
	GM.base64ToArrayBuffer = function (base64) {
		var binary_string = window.atob(base64);
		var len = binary_string.length;
		var bytes = new Uint8Array(len);
		for (var i = 0; i < len; i++) {
			bytes[i] = binary_string.charCodeAt(i);
		}
		return bytes.buffer;
	};
	GM.fileToBase64 = function (file) {
		return new Promise((resolve) => {
			const fileReader = new FileReader();
			fileReader.readAsDataURL(file);
			fileReader.onload = function () {
				resolve(this.result);
			};
		});
	};
	GM.onready = function (func) {
		/* 类似JQuery ready */
		document.addEventListener(
			"DOMContentLoaded",
			function () {
				document.removeEventListener(
					"DOMContentLoaded",
					arguments.callee,
					false
				);
				func();
			},
			false
		);
	};

	GM.mutationObserverIframe = function () {
		/* 观察iframe创建 */
		let MutationObserver =
			window.MutationObserver ||
			window.webkitMutationObserver ||
			window.MozMutationObserver;

		let config = {
			/* 当为 true 时，将会监听以 target 为根节点的整个子树。包括子树中所有节点的属性，而不仅仅是针对 target。默认值为 false */
			subtree: true,
			/* 当为 true 时，监听 target 节点中发生的节点的新增与删除（同时，如果 subtree 为 true，会针对整个子树生效）。默认值为 false。 */
			childList: true,
			/* 当为 true 时观察所有监听的节点属性值的变化。默认值为 true，当声明了 attributeFilter 或 attributeOldValue，默认值则为 false */
			attributes: undefined,
			/* 一个用于声明哪些属性名会被监听的数组。如果不声明该属性，所有属性的变化都将触发通知 */
			attributeFilter: undefined,
			/* 当为 true 时，记录上一次被监听的节点的属性变化；可查阅 MutationObserver 中的 Monitoring attribute values 了解关于观察属性变化和属性值记录的详情。默认值为 false */
			attributeOldValue: undefined,
			/* 当为 true 时，监听声明的 target 节点上所有字符的变化。默认值为 true，如果声明了 characterDataOldValue，默认值则为 false */
			characterData: undefined,
			/* 当为 true 时，记录前一个被监听的节点中发生的文本变化。默认值为 false */
			characterDataOldValue: undefined,
		};

		let mutationObserver = new MutationObserver((mutations) => {
			mutations.forEach((item) => {
				item.addedNodes.forEach((_item_) => {
					if (
						typeof _item_.nodeName === "string" &&
						_item_.nodeName.toLowerCase() === "iframe"
					) {
						console.log("观测到新增iframe1", _item_);
						GM.injectionScript(_item_);
					}
					if (_item_ instanceof MutationRecord) {
						_item_.addedNodes.forEach((__item__) => {
							if (
								typeof __item__.nodeName === "string" &&
								__item__.nodeName.toLowerCase() === "iframe"
							) {
								console.log("观测到新增iframe2", __item__);
								GM.injectionScript(__item__);
							}
						});
					} else if (_item_ instanceof Node && _item_.nodeName !== "#text") {
						_item_.querySelectorAll("iframe").forEach((__item__) => {
							if (
								typeof __item__.nodeName === "string" &&
								__item__.nodeName.toLowerCase() === "iframe"
							) {
								console.log("观测到新增iframe3", __item__);
								GM.injectionScript(__item__);
							}
						});
					}
				});
			});
		});
		mutationObserver.observe(document.documentElement, config);
	};
	/* 注册GM的回调函数 */
	/* 注册全局GM_xmlhttpRequest的onload、onabort、onerror、ontimeout的回调函数 */
	unsafeWindow.GM_INJECTION = true;
	unsafeWindow.GM_addStyle = function (styleText) {
		let cssNode = document.createElement("style");
		cssNode.setAttribute("data-gmscript-css", true);
		cssNode.setAttribute("type", "text/css");
		cssNode.innerHTML = styleText;
		document.head.appendChild(cssNode);
		return cssNode;
	};
	unsafeWindow.GM_setValue = function (key, value) {
		if (value == undefined) {
			value = null;
		}
		window.localStorage.setItem(key, JSON.stringify(value));
	};
	unsafeWindow.GM_getValue = function (key, defaultValue) {
		let value = window.localStorage.getItem(key);
		if (typeof value == "string" && value.trim() != String()) {
			value = JSON.parse(value);
		} else if (defaultValue != null) {
			value = defaultValue;
		}
		return value;
	};
	unsafeWindow.GM_deleteValue = function (key) {
		window.localStorage.removeItem(key);
	};
	unsafeWindow.GM_setClipboard = function (text, mineType = "text") {
		/* 复制到剪贴板 */
		GM.GM_setClipboard(text);
	};
	unsafeWindow.GM_xmlhttpRequest = function (options = {}) {
		/* GM_xmlhttpRequest请求 */
		return new Promise(function (resolve, reject) {
			try {
				let guid = GM.getGUID(); /* 每一个回调设置独一无二的key */
				/* 注入onabort、onerror、ontimeout的调用方法 */
				let callBackNameList = ["onabort", "onerror", "ontimeout"];
				/* onload回调单独设置 */
				GM.callback.GM_xmlhttpRequest.onload[guid] = function (response) {
					console.log(response);
					if (
						typeof response["responseBase64"] == "string" &&
						options["responseType"]?.toLowerCase() === "arraybuffer"
					) {
						let responseArray = new Uint8Array(response["responseText"].length);
						for (let i = 0; i < response["responseText"].length; i++) {
							responseArray[i] = response["responseText"].charCodeAt(i);
						}
						response["response"] = GM.base64ToArrayBuffer(
							response["responseBase64"]
						);
					} else {
						response["response"] = new DOMParser().parseFromString(
							response["responseText"],
							"text/html"
						);
					}

					response["responseXML"] = new DOMParser().parseFromString(
						response["responseText"],
						"text/xml"
					);
					options["onload"](response);
					callBackNameList.forEach(function (_item_) {
						delete GM.callback.GM_xmlhttpRequest[_item_][guid];
					});
					delete GM.callback.GM_xmlhttpRequest.onload[guid];
				};
				callBackNameList.forEach(function (item) {
					GM.callback.GM_xmlhttpRequest[item][guid] = function (response) {
						console.log(response);
						options[item](response); /* 调用自己的方法 */
						callBackNameList.forEach(function (_item_) {
							delete GM.callback.GM_xmlhttpRequest[_item_][guid];
						});
						delete GM.callback.GM_xmlhttpRequest.onload[guid];
					};
				});

				if (!options.url.match(/^http/gi)) {
					if (!options.url.match(/^\//gi)) {
						options.url = "/" + options.url;
					}
					options.url = location.origin + options.url;
				}
				if (
					options.data &&
					options.data != "" &&
					typeof options.data === "string"
				) {
					/* 重新格式化一下data */
					try {
						options.data = JSON.parse(options.data);
					} catch (error) {
						let optionsData = {};
						options.data.split("&").forEach((item) => {
							let optionDataKey = item.match(/^(.+)=/i);
							optionDataKey = optionDataKey[optionDataKey.length - 1];
							let optionDataValue = item.match(/^.+=(.*)/i);
							optionDataValue = optionDataValue
								? optionDataValue[optionDataValue.length - 1]
								: "";
							optionDataValue = decodeURIComponent(optionDataValue);
							optionsData[optionDataKey] = optionDataValue;
						});
						options.data = optionsData;
					}
				}
				if (options.data && options.data instanceof FormData) {
					let optionsDataEntries = options.data.entries();
					let optionsData = {};
					while (1) {
						let optionsDataEntriesItem = optionsDataEntries.next();
						if (optionsDataEntriesItem.value) {
							let optionDataKey = optionsDataEntriesItem.value[0];
							let optionDataValue = optionsDataEntriesItem.value[1];
							optionsData[optionDataKey] = optionDataValue;
						} else {
							break;
						}
					}
					options.data = optionsData;
				}
				let _options_ = {
					guid: guid,
					url: options.url,
					method: options.method ? options.method : "get",
					timeout: options.timeout ? options.timeout : 0 /* 单位：毫秒 */,
					headers: options.headers ? options.headers : {},
					responseType: options.responseType ? options.responseType : "html",
					data: options.data ? options.data : "",

					from: "top",
					cookie: options.cookie ? options.cookie : "",
					files: {},
				};
				Promise.all(
					Object.keys(_options_.data).map(async (key) => {
						if (_options_.data[key] instanceof File) {
							console.log("存在文件");
							let fileName = _options_.data[key].name; /* 文件名 */
							let fileType = _options_.data[key].type; /* 文件类型 */
							let fileSize = _options_.data[key].size; /* 文件大小 */
							let fileKey =
								"isFileInOptionsFiles_" + key; /* 原File对象变为对应的id */
							_options_.files[fileKey] = {
								fileName: fileName,
								fileType: fileType,
								fileSize: fileSize,
							};
							_options_.data[key] = fileKey;
						}
					})
				).then(() => {
					console.log(_options_);
					_options_ = JSON.stringify(_options_); /* 转换为字符串传递参数 */
					console.log(_options_);
					GM.GM_xmlhttpRequest(_options_);
					resolve(); /* 回调 */
				});
			} catch (error) {
				reject(error); /* 回调 */
			}
		});
	};
	unsafeWindow.GM_registerMenuCommand = function (showText = "", callback) {
		/* 注册菜单 */
		let guid = GM.getGUID();
		GM.callback.GM_menuCommand[guid] = {
			changeEvent: function () {
				callback();
			},
			showText: showText,
		};
		let params = { guid: guid, showText: showText };
		params = JSON.stringify(params);
		GM.GM_registerMenuCommand(params);
		return guid;
	};
	unsafeWindow.GM_unregisterMenuCommand = function (menuGUID) {
		/* 取消注册菜单 */
		if (typeof menuGUID !== "string") {
			return;
		}
		GM.GM_unregisterMenuCommand(menuGUID);
		delete GM.callback.GM_menuCommand[menuGUID];
	};
	/* GM.onready(() => {
		GM.mutationObserverIframe();
	}); */
})(_GM_);
