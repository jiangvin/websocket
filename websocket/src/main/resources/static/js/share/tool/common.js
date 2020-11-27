/**
 * @author 蒋文龙(Vin)
 * @description
 * @date 2020/5/26
 */

import Resource from "./resource.js";

export default class Common {
    constructor() {
    }

    static addMessage(context, color) {
        Resource.getRoot().addMessage(context, color);
    }

    static nextStage(options) {
        Resource.getRoot().nextStage(options);
    }

    static lastStage(options) {
        Resource.getRoot().lastStage(options);
    }

    static gotoStage(id, options) {
        Resource.getRoot().gotoStage(id, options);
    }

    static addTimeEvent(eventType, callBack, timeout, ignoreLog) {
        Resource.getRoot().addTimeEvent(eventType, callBack, timeout, ignoreLog);
    }

    static addMessageEvent(eventType, callBack) {
        Resource.getRoot().addMessageEvent(eventType, callBack);
    }

    static syncUserData() {
        if (!Resource.getUser().deviceId) {
            return;
        }

        const oldCoins = Resource.getUser().coin;
        const oldStage = Resource.getUser().stage;
        Common.getRequest("/user/getUser?userId=" + Resource.getUser().deviceId, function (data) {
            Resource.setUser(data);
            const newCoins = Resource.getUser().coin;
            if (newCoins > oldCoins) {
                Common.addMessage("获得金币数量: " + (newCoins - oldCoins));
            }
            const newStage = Resource.getUser().stage;
            if (newStage > oldStage) {
                Common.addMessage("解锁新的任务关卡!");
            }
        });
    }

    static getRequest(url, callBack) {
        try {
            const xmlHttp = new XMLHttpRequest();
            xmlHttp.onreadystatechange = function () {
                if (xmlHttp.readyState !== 4) {
                    return;
                }
                if (xmlHttp.status !== 200) {
                    Common.addMessage(xmlHttp.responseText, "#ff0000");
                    return;
                }

                const result = JSON.parse(xmlHttp.responseText);
                if (result.success) {
                    callBack(result.data);
                } else {
                    Common.addMessage(result.message, "#ff0000");
                }
            };
            xmlHttp.open("GET", Common.generateHttpHost() + encodeURI(url), true); // true for asynchronous
            xmlHttp.send(null);
        } catch (e) {
            Common.addMessage(e, "#ff0000");
        }
    }

    static postEncrypt(url, body, callback) {
        this.postRequest(url, {data: Resource.encryptData(body)}, callback);
    }

    static postRequest(url, body, callback) {
        try {
            const xmlHttp = new XMLHttpRequest();
            xmlHttp.onreadystatechange = function () {
                if (xmlHttp.readyState !== 4) {
                    return;
                }
                if (xmlHttp.status !== 200) {
                    Common.addMessage(xmlHttp.responseText, "#ff0000");
                    return;
                }

                const result = JSON.parse(xmlHttp.responseText);
                if (result.success) {
                    if (callback) {
                        callback(result.data);
                    }
                } else {
                    Common.addMessage(result.message, "#ff0000");
                }
            };
            xmlHttp.open("POST", Common.generateHttpHost() + encodeURI(url), true); // true for asynchronous
            xmlHttp.setRequestHeader('content-type', 'application/json');
            xmlHttp.send(JSON.stringify(body));
        } catch (e) {
            Common.addMessage(e, "#ff0000");
        }
    };

    static generateHttpHost() {
        return Resource.getHost() === "" ? document.location.href.split("?")[0] : Resource.getHost();
    }

    static distance(x1, y1, x2, y2) {
        return Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2));
    };

    static width() {
        return Resource.width();
    }

    static height() {
        return Resource.height();
    }

    static getPositionFromId(id) {
        const position = {};
        const infos = id.split("_");
        const size = Resource.getUnitSize();
        position.x = parseInt(infos[0]) * size + size / 2;
        position.y = parseInt(infos[1]) * size + size / 2;
        return position;
    };

    static getIdFromPosition(pos) {
        const size = Resource.getUnitSize();
        return Math.floor(pos.x / size) + "_" + Math.floor(pos.y / size);
    }
}

CanvasRenderingContext2D.prototype.displayCenter = function (imageId, x, y, w, h) {
    this.displayBase(imageId, x, y, w, h, "center");
};

CanvasRenderingContext2D.prototype.displayTopLeft = function (imageId, x, y, w, h) {
    this.displayBase(imageId, x, y, w, h, "topLeft");
};

CanvasRenderingContext2D.prototype.displayCenterRate = function (imageId, rateX, rateY, rateW, rateH) {
    this.displayRate(imageId, rateX, rateY, rateW, rateH, "center");
};

CanvasRenderingContext2D.prototype.displayRate = function (imageId, rateX, rateY, rateW, rateH, align) {
    const img = Resource.getImage(imageId);
    if (!img) {
        return;
    }

    let w;
    if (rateW) {
        w = Resource.displayW() * rateW;
    } else {
        //未加载完毕的情况
        if (!img.width) {
            return;
        }
        w = img.width;
    }

    let h;
    if (rateH) {
        h = Resource.displayH() * rateH;
    } else {
        //未加载完毕的情况
        if (!img.width || !img.height) {
            return;
        }
        h = w * img.height / img.width;
    }

    const x = Resource.displayW() * rateX;
    const y = Resource.displayH() * rateY;

    this.displayBase(imageId, x, y, w, h, align);
};

CanvasRenderingContext2D.prototype.displayBase = function (imageId, x, y, w, h, align) {
    const img = Resource.getImage(imageId);
    if (!img) {
        return;
    }

    if (!h) {
        //未加载完毕的情况
        if (!img.width || !img.height) {
            return;
        }
        h = w * img.height / img.width;
    }

    //全屏模式的优化处理
    if (Math.abs(Resource.width() - w) < 2 &&
        Math.abs(Resource.height() - h) < 2) {
        w = Resource.width();
        h = Resource.height();
    } else {
        x = x + Resource.getOffset().x;
        y = y + Resource.getOffset().y;
    }

    switch (align) {
        case "center":
            x = x - w / 2;
            y = y - h / 2;
            break;
    }

    this.drawImage(img,
        0, 0,
        img.width, img.height,
        x, y,
        w, h);
};

Date.prototype.format = function (fmt) {
    const o = {
        "M+": this.getMonth() + 1,                      //月份
        "d+": this.getDate(),                           //日
        "h+": this.getHours(),                          //小时
        "m+": this.getMinutes(),                        //分
        "s+": this.getSeconds(),                        //秒
        "q+": Math.floor((this.getMonth() + 3) / 3), //季度
        "S": this.getMilliseconds()                     //毫秒
    };
    if (/(y+)/.test(fmt)) {
        fmt = fmt.replace(RegExp.$1, (this.getFullYear() + "").substr(4 - RegExp.$1.length));
    }
    for (let k in o) {
        if (new RegExp("(" + k + ")").test(fmt)) {
            fmt = fmt.replace(RegExp.$1, (RegExp.$1.length === 1) ? (o[k]) : (("00" + o[k]).substr(("" + o[k]).length)));
        }
    }
    return fmt;
};
