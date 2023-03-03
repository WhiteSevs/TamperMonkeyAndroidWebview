# TamperMonkeyAndroidWebview

在Android的`WebView`上使用油猴函数的一个项目，也是一个脚本管理，其中示例使用了`MT论坛`

# 目前项目仍在编写中

+ 脚本数据存储（待完成）
+ 脚本管理界面（待完成）
+ 新建脚本的编写界面（待完成）
+ 脚本的设置界面（待完成）
+ 设置界面（待完成）
+ 新WebView中的`input`上传文件新选择器（待完成）
+ 脚本的`@run-at`为`document-start`、`document-body`、`document-end`、`document-idle`和`context-menu`
  的执行时间插入（待完成）
+ 脚本的`input`上传文件类型`mimeType`过滤（待完成）
+ 油猴的函数完善（待完成）
+ 等......（待完成）

## 其中

```javascript
@run-at document-start 👇
<html xmlns="http://www.w3.org/1999/xhtml">
    <head>
        <!-- 脚本注入到这里 -->
        ...
    </head>
</html>


@run-at document-body 👇
<html xmlns="http://www.w3.org/1999/xhtml">
    <head>
        ...
    </head>
    <body>
        ...
    </body>
    <!-- 脚本注入到这里 -->
</html>


@run-at document-end 👇
<html xmlns="http://www.w3.org/1999/xhtml">
    <head>
        ...
    </head>
    <body>
        ...
    </body>
</html>
<!-- 脚本注入到这里，html后面也会执行 -->

@run-at document-idle 👇
使用 document.onload 执行


@run-at context-menu 👇
点击菜单才执行脚本

```

蓝奏云地址: [https://baiqi.lanzoul.com/b06753gjc](https://baiqi.lanzoul.com/b06753gjc)
蓝奏云密码：chmn