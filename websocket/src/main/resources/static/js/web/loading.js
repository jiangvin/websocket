/**
 * @author 蒋文龙(Vin)
 * @description 资源加载的缓冲页面
 * @date 2020/7/3
 */
import Stage from "../share/stage/stage.js";
import Resource from "../share/tool/resource.js";
import Sound from "../share/tool/sound.js";

export default class Loading extends Stage {
    constructor() {
        super();

        this.percent = 0;
        this.isInit = false;

        //background
        this.createItem({
            draw: function (ctx) {
                ctx.fillStyle = '#01A7EC';
                ctx.fillRect(0, 0, Resource.width(), Resource.height());
            }
        });

        //logo
        this.createItem({
            draw: function (ctx) {
                ctx.displayCenterRate("logo",
                    .5,
                    .45,
                    .55);
            }
        });

        //progress bar
        this.createItem({
            draw: ctx => {
                const width = Resource.formatWidth() * .4;
                const height = 50;
                const pos = {
                    x: Resource.formatWidth() * .5 - width / 2,
                    y: Resource.formatHeight() * .6
                };

                //进度条背景
                ctx.fillStyle = '#FFF';
                ctx.displayFillRoundRect(
                    pos.x,
                    pos.y,
                    width,
                    height,
                    height / 2);

                //进度条
                ctx.fillStyle = '#028EE7';
                ctx.displayFillRoundRect(
                    pos.x,
                    pos.y,
                    width * (this.percent / 100.0),
                    height,
                    height / 2);

                //文字
                ctx.textAlign = 'center';
                ctx.textBaseline = 'top';
                ctx.fillStyle = '#FFF';
                ctx.displayText('资源已加载' + this.percent + '%',
                    pos.x + width / 2,
                    pos.y + 65,
                    36);
            }
        });

        this.initEvent();
    }

    initEvent() {
        const total = Resource.instance.images.size + Sound.instance.sounds.size;
        let loaded = 0;

        const event = () => {
            ++loaded;
            this.percent = Math.floor(loaded * 100 / total);
            if (this.percent >= 100 && this.isInit) {
                Resource.getRoot().nextStage();
            }
        };

        //加载图片
        Resource.instance.images.forEach(function (image) {
            if (image.complete) {
                event();
            } else {
                image.onload = function () {
                    event();
                }
            }
        });

        //加载音频
        createjs.Sound.alternateExtensions = ["mp3", "wav"];
        createjs.Sound.on("fileload", event, this);
        Sound.instance.sounds.forEach(function (sound) {
            createjs.Sound.registerSound(sound.src, sound.id);
            sound.play = function () {
                if (sound.loop) {
                    createjs.Sound.play(sound.id, {loop: -1});
                } else {
                    createjs.Sound.play(sound.id);
                }
            };
            sound.stop = function () {
                createjs.Sound.stop(sound.id);
            };
        });

        //实现声音函数
        createjs.Sound.volume = Sound.instance.volume;
        Sound.instance.setVolumeEngine = function (volume) {
            createjs.Sound.volume = volume;
        };

        //切换至后台时静音
        const handleVisibilityChange = () => {
            if (document.hidden) {
                //记录开始时间
                this.startTime = new Date().getTime();
                createjs.Sound.volume = 0;
            } else {
                createjs.Sound.volume = Sound.instance.volume;

                //检测时间，如果超过5分钟则重启
                //TODO - 在安卓中会失效，暂无解决方案
                const currentTime = new Date().getTime();
                if (currentTime - this.startTime >= 5 * 60 * 1000) {
                    document.location.reload();
                }
            }
        };
        document.addEventListener("visibilitychange", handleVisibilityChange);
    }

    init() {
        if (this.percent >= 100) {
            Resource.getRoot().nextStage();
            return;
        }
        this.isInit = true;

        //每5秒检测一次，若5秒进度条未更新则直接进入
        let lastPercent = this.percent;
        const checkPercent = () => {
            //防止跳转后再跳转
            if (this.percent >= 100) {
                return;
            }

            if (this.percent > lastPercent) {
                //进度条已更新
                lastPercent = this.percent;
                setTimeout(checkPercent, 5000);
            } else {
                //进度条未更新,直接进入
                Resource.getRoot().nextStage();
            }
        };
        setTimeout(checkPercent, 5000);
    }
}