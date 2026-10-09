/**
 * 把生成的 JPG 图标转成透明背景的 PNG
 * 使用 jimp v3+ API
 */
const { Jimp } = require('jimp')
const fs = require('fs')
const path = require('path')

const dir = path.join(__dirname, 'src/static/tabbar')
const icons = ['home', 'channel', 'message', 'mine']
const suffixes = ['', '-active']

async function processIcon(name, suffix) {
  const inputPath = path.join(dir, `${name}${suffix}.jpg`)
  const outputPath = path.join(dir, `${name}${suffix}.png`)

  if (!fs.existsSync(inputPath)) {
    console.log(`跳过（文件不存在）: ${name}${suffix}.jpg`)
    return
  }

  const image = await Jimp.read(inputPath)
  const w = image.width
  const h = image.height

  // 把白色背景变透明
  for (let y = 0; y < h; y++) {
    for (let x = 0; x < w; x++) {
      const idx = (y * w + x) * 4
      const r = image.bitmap.data[idx]
      const g = image.bitmap.data[idx + 1]
      const b = image.bitmap.data[idx + 2]
      // 接近白色就设为透明
      if (r > 240 && g > 240 && b > 240) {
        image.bitmap.data[idx + 3] = 0
      }
    }
  }

  // 裁剪：找到图标实际边界
  let minX = w, minY = h, maxX = 0, maxY = 0
  for (let y = 0; y < h; y++) {
    for (let x = 0; x < w; x++) {
      const idx = (y * w + x) * 4
      const a = image.bitmap.data[idx + 3]
      if (a > 0) {
        if (x < minX) minX = x
        if (y < minY) minY = y
        if (x > maxX) maxX = x
        if (y > maxY) maxY = y
      }
    }
  }

  // 加 10% 边距
  const iconW = maxX - minX + 1
  const iconH = maxY - minY + 1
  const padding = Math.floor(Math.max(iconW, iconH) * 0.1)
  minX = Math.max(0, minX - padding)
  minY = Math.max(0, minY - padding)
  maxX = Math.min(w - 1, maxX + padding)
  maxY = Math.min(h - 1, maxY + padding)

  const finalW = maxX - minX + 1
  const finalH = maxY - minY + 1
  const size = Math.max(finalW, finalH)

  // 创建正方形画布
  const square = new Jimp({ width: size, height: size, color: 0x00000000 })

  // 把图标画到中央
  const offsetX = Math.floor((size - finalW) / 2)
  const offsetY = Math.floor((size - finalH) / 2)

  for (let y = 0; y < finalH; y++) {
    for (let x = 0; x < finalW; x++) {
      const srcIdx = ((minY + y) * w + (minX + x)) * 4
      const dstIdx = ((offsetY + y) * size + (offsetX + x)) * 4
      square.bitmap.data[dstIdx] = image.bitmap.data[srcIdx]
      square.bitmap.data[dstIdx + 1] = image.bitmap.data[srcIdx + 1]
      square.bitmap.data[dstIdx + 2] = image.bitmap.data[srcIdx + 2]
      square.bitmap.data[dstIdx + 3] = image.bitmap.data[srcIdx + 3]
    }
  }

  // 缩放到 128x128
  square.resize({ w: 128, h: 128 })

  // 保存 PNG
  await square.write(outputPath)
  console.log(`✅ 生成: ${name}${suffix}.png`)
}

async function main() {
  console.log('')
  console.log('Processing tabbar icons...')
  console.log('')

  for (const name of icons) {
    for (const suffix of suffixes) {
      await processIcon(name, suffix)
    }
  }

  console.log('')
  console.log('Done! Icons saved to src/static/tabbar/')
  console.log('')
}

main().catch(console.error)
