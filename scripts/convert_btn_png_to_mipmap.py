
import os
import argparse
from PIL import Image

def generate_android_assets(image_path, output_name, base_output_dir):
    # 自动补全后缀
    if not output_name.lower().endswith(('.png', '.jpg', '.jpeg', '.webp')):
        output_name += '.png'

    # 5个精确尺寸对应 Android 标准的 5 个 mipmap 分辨率目录
    size_mapping = {
        (1189, 399): "mipmap-xxxhdpi",
        (1029, 345): "mipmap-xxhdpi",
        (869, 291):  "mipmap-xhdpi",
        (789, 264):  "mipmap-hdpi",
        (709, 237):  "mipmap-mdpi"
    }

    if not os.path.exists(image_path):
        print(f"❌ 错误：找不到源图片文件 '{image_path}'")
        return

    try:
        img = Image.open(image_path)
        print(f"🚀 开始处理，输出根目录: {base_output_dir}")
        
        for (width, height), folder_name in size_mapping.items():
            # 拼接相对路径并创建文件夹
            target_dir = os.path.join(base_output_dir, folder_name)
            os.makedirs(target_dir, exist_ok=True)
            
            # 缩放并保存
            target_file_path = os.path.join(target_dir, output_name)
            resized_img = img.resize((width, height), Image.Resampling.LANCZOS)
            resized_img.save(target_file_path)
            print(f"  -> 已生成: {target_file_path} ({width}x{height})")
            
        print("✨ 所有尺寸图片已成功分发完毕！")

    except Exception as e:
        print(f"❌ 处理失败: {e}")

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Android mipmap 自动切图与分发工具")
    
    # 添加命令行参数
    parser.add_argument("-i", "--input", required=True, help="需要修改的源图片路径 (例如: ./test.png)")
    parser.add_argument("-o", "--output", required=True, help="输出图片的文件名 (例如: ic_btn_test.png)")
    parser.add_argument("-d", "--dir", default="./res", help="输出的相对根路径 (默认: ./res)")

    args = parser.parse_args()

    generate_android_assets(args.input, args.output, args.dir)
