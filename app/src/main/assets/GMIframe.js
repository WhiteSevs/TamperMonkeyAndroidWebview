(function (GM) {
	/* 本js主要是再frame里创建iframe时调用 */
	if (typeof window.GM_INJECTION !== "undefined") {
		console.log("GM API 已注入过");
		return;
	}
	window.unsafeWindow = window;
	let iframe_mutationObserverIframe = function () {
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
	/* 注册GM的函数 */
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
				/* 注入onload、onabort、onerror、ontimeout的调用方法 */
				let callBackNameList = ["onabort", "onerror", "ontimeout"];
				GM.callback.GM_xmlhttpRequest_iframe.onload[guid] = function (
					response
				) {
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
						delete top
							.window._GM_.callback.GM_xmlhttpRequest_iframe[_item_][guid];
					});
					delete GM.callback.GM_xmlhttpRequest_iframe.iframeNode[guid];
					delete GM.callback.GM_xmlhttpRequest_iframe.onload[guid];
				};
				callBackNameList.forEach(function (item) {
					GM.callback.GM_xmlhttpRequest_iframe[item][guid] = function (
						response
					) {
						console.log(response);
						options[item](response);
						callBackNameList.forEach(function (_item_) {
							delete top
								.window._GM_.callback.GM_xmlhttpRequest_iframe[_item_][guid];
						});
						delete GM.callback.GM_xmlhttpRequest_iframe.iframeNode[guid];
						delete GM.callback.GM_xmlhttpRequest_iframe.onload[guid];
					};
				});
				if (!options.url.match(/^http/gi)) {
					/* 如果没设置http */
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
							let optionDataValue = item.match(/^.+=(.+)/i);
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
					url: options.url,
					method: options.method ? options.method : "get",
					data: options.data ? options.data : "",
					headers: options.headers ? options.headers : {},
					responseType: options.responseType ? options.responseType : "html",
					guid: guid,
					timeout: options.timeout ? options.timeout : 0 /* 单位：毫秒 */,
					from: "iframe",
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
		top.window.GM_menuCommand_callBack[guid] = {
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
		delete top.window.GM_menuCommand_callBack[menuGUID];
	};
	iframe_mutationObserverIframe();
})(top.window._GM_);
