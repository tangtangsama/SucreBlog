package cn.sucrelt.sucreblog.controller.admin;

import cn.sucrelt.sucreblog.config.ConstantsConfig;
import cn.sucrelt.sucreblog.entity.Blog;
import cn.sucrelt.sucreblog.service.BlogService;
import cn.sucrelt.sucreblog.service.CategoryService;
import cn.sucrelt.sucreblog.util.MyBlogUtils;
import cn.sucrelt.sucreblog.util.Result;
import cn.sucrelt.sucreblog.util.ResultGenerator;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;


/**
 * @description:
 * @author: sucre
 * @date: 2021/01/09
 * @time: 13:22
 */

@Controller
@RequestMapping("/admin")
public class BlogController {

    @Resource
    private BlogService blogService;

    @Resource
    private CategoryService categoryService;


    /**
     * GET方式请求博客添加界面
     *
     * @param request
     * @return
     */
    @GetMapping("/blogs/edit")
    public String edit(HttpServletRequest request) {
        request.setAttribute("path", "edit");
        request.setAttribute("categories", categoryService.getAllCategories());
        return "admin/edit";
    }

    /**
     * GET方式请求修改某一篇博客并回显博客
     *
     * @param request
     * @param blogId
     * @return
     */
    public String edit(HttpServletRequest request, @PathVariable("blogId") Long blogId) {
        request.setAttribute("path", "edit");
        Blog blog = blogService.getBlogById(blogId);
        if (blog == null) {
            return "error/error_400";
        }
        request.setAttribute("blog", blog);
        request.setAttribute("categories", categoryService.getAllCategories());
        return "admin/edit";
    }

    /**
     * 上传博客图片文件
     *
     * @param request
     * @param response
     * @param file
     * @throws IOException
     * @throws URISyntaxException
     */
    @PostMapping("/blogs/md/uploadfile")
    public void uploadFileByEditorMd(HttpServletRequest request, HttpServletResponse response,
                                     @RequestParam(name = "editormd-image-file", required = true) MultipartFile file) throws IOException, URISyntaxException {
        // 获取文件前缀
        String fileName = file.getOriginalFilename();
        String suffixName = fileName.substring(fileName.lastIndexOf("."));

        // 生成文件名
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
        Random r = new Random();
        StringBuilder tempName = new StringBuilder();
        tempName.append(simpleDateFormat.format(new Date())).append(r.nextInt(100)).append(suffixName);
        String newFileName = tempName.toString();

        // 文件上传位置
        File destFile = new File(ConstantsConfig.FILE_UPLOAD_DIC + newFileName);
        String fileUrl = MyBlogUtils.getHost(new URI(request.getRequestURL() + "")) + "/upload/" + newFileName;
        File fileDirectory = new File(ConstantsConfig.FILE_UPLOAD_DIC);
        try {
            if (!fileDirectory.exists()) {
                if (!fileDirectory.mkdir()) {
                    throw new IOException("文件夹创建失败,路径为：" + fileDirectory);
                }
            }
            file.transferTo(destFile);
            request.setCharacterEncoding("utf-8");
            response.setHeader("Content-Type", "text/html");
            response.getWriter().write("{\"success\": 1, \"message\":\"success\",\"url\":\"" + fileUrl + "\"}");
        } catch (UnsupportedEncodingException e) {
            response.getWriter().write("{\"success\":0}");
        } catch (IOException e) {
            response.getWriter().write("{\"success\":0}");
        }
    }

    /**
     * 添加一篇博客
     *
     * @param blogTitle
     * @param blogSubUrl
     * @param blogCategoryId
     * @param blogTags
     * @param blogContent
     * @param blgCoverImage
     * @param blogStatus
     * @param enableComment
     * @return
     */
    @PostMapping("/blogs/save")
    @ResponseBody
    public Result addBlog(@RequestParam("blogTitle") String blogTitle,
                          @RequestParam(name = "blogSubUrl", required = false) String blogSubUrl,
                          @RequestParam("blogCategoryId") Integer blogCategoryId,
                          @RequestParam("blogTags") String blogTags,
                          @RequestParam("blogContent") String blogContent,
                          @RequestParam("blogCoverImage") String blgCoverImage,
                          @RequestParam("blogStatus") Byte blogStatus,
                          @RequestParam("enableComment") Byte enableComment) {
        if (blogTitle.isEmpty()) {
            return ResultGenerator.generateFailResult("请输入文章标题！");
        }
        if (blogTitle.trim().length() > 150) {
            return ResultGenerator.generateFailResult("文章标题过长！");
        }
        if (blogTags.isEmpty()) {
            return ResultGenerator.generateFailResult("请输入文章标签！");
        }
        if (blogTags.trim().length() > 150) {
            return ResultGenerator.generateFailResult("标签过长！");
        }
        if (blogSubUrl.trim().length() > 150) {
            return ResultGenerator.generateFailResult("文章路径过长！");
        }
        if (blogContent.isEmpty()) {
            return ResultGenerator.generateFailResult("请输入文章内容！");
        }
        if (blogContent.trim().length() > 100000) {
            return ResultGenerator.generateFailResult("文章内容过长！");
        }
        if (blgCoverImage.isEmpty()) {
            return ResultGenerator.generateFailResult("封面图不能为空！");
        }

        Blog blog = new Blog();
        blog.setBlogTitle(blogTitle);
        blog.setBlogSubUrl(blogSubUrl);
        blog.setBlogCategoryId(blogCategoryId);
        blog.setBlogTags(blogTags);
        blog.setBlogContent(blogContent);
        blog.setBlogCoverImage(blgCoverImage);
        blog.setBlogStatus(blogStatus);
        blog.setEnableComment(enableComment);

        String saveBlogResult = blogService.saveBlog(blog);
        if ("success".equals(saveBlogResult)) {
            return ResultGenerator.generateSuccessResult("添加成功");
        } else {
            return ResultGenerator.generateFailResult(saveBlogResult);
        }
    }

    @PostMapping("/blogs/update")
    @ResponseBody
    public Result update(@RequestParam("blogId") Long blogId,
                         @RequestParam("blogTitle") String blogTitle,
                         @RequestParam(name = "blogSubUrl", required = false) String blogSubUrl,
                         @RequestParam("blogCategoryId") Integer blogCategoryId,
                         @RequestParam("blogTags") String blogTags,
                         @RequestParam("blogContent") String blogContent,
                         @RequestParam("blogCoverImage") String blgCoverImage,
                         @RequestParam("blogStatus") Byte blogStatus,
                         @RequestParam("enableComment") Byte enableComment) {
        if (blogTitle.isEmpty()) {
            return ResultGenerator.generateFailResult("请输入文章标题！");
        }
        if (blogTitle.trim().length() > 150) {
            return ResultGenerator.generateFailResult("文章标题过长！");
        }
        if (blogTags.isEmpty()) {
            return ResultGenerator.generateFailResult("请输入文章标签！");
        }
        if (blogTags.trim().length() > 150) {
            return ResultGenerator.generateFailResult("标签过长！");
        }
        if (blogSubUrl.trim().length() > 150) {
            return ResultGenerator.generateFailResult("文章路径过长！");
        }
        if (blogContent.isEmpty()) {
            return ResultGenerator.generateFailResult("请输入文章内容！");
        }
        if (blogContent.trim().length() > 100000) {
            return ResultGenerator.generateFailResult("文章内容过长！");
        }
        if (blgCoverImage.isEmpty()) {
            return ResultGenerator.generateFailResult("封面图不能为空！");
        }

        Blog blog = new Blog();
        blog.setBlogId(blogId);
        blog.setBlogTitle(blogTitle);
        blog.setBlogSubUrl(blogSubUrl);
        blog.setBlogCategoryId(blogCategoryId);
        blog.setBlogTags(blogTags);
        blog.setBlogContent(blogContent);
        blog.setBlogCoverImage(blgCoverImage);
        blog.setBlogStatus(blogStatus);
        blog.setEnableComment(enableComment);

        String updateBlogResult = blogService.updateBlog(blog);
        if ("success".equals(updateBlogResult)) {
            return ResultGenerator.generateSuccessResult("添加成功");
        } else {
            return ResultGenerator.generateFailResult(updateBlogResult);
        }
    }
}