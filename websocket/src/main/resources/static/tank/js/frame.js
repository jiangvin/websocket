/**
 * @author 蒋文龙(Vin)
 * @description
 * @date 2020/5/12
 */

function Frame() {
    this.frames = 60;
    this.totalFrames = 0;
    this.lastFrames = 0;
    this.lastTime = Date.now();

    /**
     * 计算帧数
     */
    this.calculate = function () {
        ++this.totalFrames;
        const offset = Date.now() - this.lastTime;
        if (offset >= 1000) {
            this.frames = this.totalFrames - this.lastFrames;
            this.lastFrames = this.totalFrames;
            this.lastTime += offset;
        }
    }
}