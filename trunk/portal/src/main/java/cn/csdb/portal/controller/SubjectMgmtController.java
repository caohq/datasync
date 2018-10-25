package cn.csdb.portal.controller;

import cn.csdb.portal.model.Subject;
import cn.csdb.portal.service.SubjectMgmtService;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.util.List;

@Controller
@RequestMapping("/subjectMgmt")
public class SubjectMgmtController {
    private SubjectMgmtService subjectService;
    private static final Logger logger = LogManager.getLogger(SubjectMgmtController.class);

    @Autowired
    public void setProjectLibService(SubjectMgmtService subjectService) {
        this.subjectService = subjectService;
    }

    /**
     * Function Description:
     *
     * @param subject, the wrapped object which contains information of the subject ot be added
     * @param image,   the image field of input form
     * @return redirectStr, the request is redirected to querySubject interface
     * @author zzl
     * @date 2018/10/23
     */
    @RequestMapping(value = "/addSubject", method = RequestMethod.POST)
    public String addSubject(HttpServletRequest request, Subject subject, @RequestParam("image") MultipartFile image) {
        //input parameter check
        logger.info("enterring subjectMgmt-addSubject.");
        logger.info(subject);
        logger.info("image(" + image + ")");

        //save image
        logger.info("saving image");
        String imagePath = saveImage(image);
        subject.setImagePath(imagePath);
        logger.info("image saved");

        String addSubjectNotice = subjectService.addSubject(subject);
        logger.info("subject added, addSubjectNotice : " + addSubjectNotice);

        int totalPages = subjectService.getTotalPages();
        String redirectStr = "redirect:/subjectMgmt/querySubject?currentPage=" + totalPages;
        logger.info("redirect request to querySubject : " + redirectStr);
        return redirectStr;
    }

    /**
     * Function Description: save image binary data to local filesystem and return the path
     *
     * @param image， the image to be stored
     * @return imageFilePath, the absolute local filesystem path of the image, may be a path like {portalRoot}/SubjectImages/img1.jpg, here {portalRoot} represent the root of the web app
     */
    private String saveImage(MultipartFile image) {
        logger.info("save image file, image = " + image);
        String imagesPath = "/SubjectImages/";
        String fileName = image.getOriginalFilename();

        // check the input file's path to ensure user input a real image
        String imageFilePath = "";
        if (!(fileName == null || fileName.equals(""))) {
            imageFilePath = imagesPath + fileName;
        } else {
            return "";
        }

        // if the /SubjectImages/ directory doesn't exsit, then create it.
        File imageFilePathObj = new File(imageFilePath);
        if (!imageFilePathObj.getParentFile().exists()) {
            imageFilePathObj.getParentFile().mkdirs();
        }

        // store the image to local filesystem
        try {
            image.transferTo(imageFilePathObj);
        } catch (Exception e) {
            e.printStackTrace();
        }

        logger.info("image saved path : " + imageFilePath);
        logger.info("save image file completed!");

        return imageFilePathObj.getAbsolutePath();
    }

    /**
     * Function Description:
     *
     * @param id, the id of Subject to be deleted
     * @param currentPage, the page which contains the deleted subject, this parameter is designed for request redirect
     * @return redirectStr, the request is redirected to querySubject interface
     * @author zzl
     * @date 2018/10/23
     */
    @RequestMapping(value = "/deleteSubject")
    public String deleteSubject(HttpServletRequest request, @RequestParam(required = true) String id, @RequestParam(required = true) int currentPage) {
        logger.info("SubjectMgmtController-deleteSubject, id = " + id + ", currentPage = " + currentPage);

        String deleteSubjectNotice = subjectService.deleteSubject(id);
        logger.info("SubjectMgmtController-deleteSubject，deleteSubjectNotice = " + deleteSubjectNotice);

        String redirectStr = "redirect:/subjectMgmt/querySubject?currentPage=" + currentPage;
        logger.info("redirect request to querySubject : " + redirectStr);

        return redirectStr;
    }

    /**
     * Function Description:
     *
     * @param subject the subject to be updated
     * @param image the image field
     * @return redirectStr, the request is redirected to querySubject interface
     * @author zzl
     * @date 2018/10/23
     */
    @RequestMapping(value = "/updateSubject")
    public String updateSubject(HttpServletRequest request, Subject subject, @RequestParam("image") MultipartFile image) {
        logger.info("SubjectMgmtController-updateSubject");
        logger.info("SubjectMgmtController-updateSubject -" + subject);
        logger.info("SubjectMgmtController-updateSubject - MultiparFile = " + image + ", fileName = " + image.getOriginalFilename());
        logger.info("updating image");
        updateImage(subject, image);
        logger.info("updated image");
        String updateSubjectNotice = subjectService.updateSubject(subject);
        logger.info("update subject completed. updateSubjectNotice = " + updateSubjectNotice);

        String redirectStr = "redirect:/subjectMgmt/querySubject?currentPage=1";
        logger.info("redirect request to querySubject : " + redirectStr);

        return redirectStr;
    }

    /**
     *  Function Description: update image is designed to save the file user selected on UpdateSubjectDialog
     *
     * @param subject the subject to be updated
     * @param image the image to be saved
     * @return imagePath the absolute local filesystem path of the image
     * @author zzl
     * @date 2018/10/23
     */
    private String updateImage(Subject subject, MultipartFile image) {
        logger.info("updating image");

        String imagePath = "";

        Subject tmpSubject = subjectService.findSubjectById(subject.getId());
        if ((image != null) && (image.getOriginalFilename() != "")) {
            deleteImage(tmpSubject.getImagePath());
            imagePath = saveImage(image);
        } else {
            imagePath = tmpSubject.getImagePath();
        }

        return imagePath;
    }

    /**
     *  Function Description: when user select a image on UpdateSubjectDialog, we must delete the previous one before store the new image
     * @param imagePath
     * @author zzl
     * @date 2018/10/23
     */
    private void deleteImage(String imagePath) {
        logger.info("updateSubject - image path to be deleted : " + imagePath);

        try {
            File imagePathFileObj = new File(imagePath);
            imagePathFileObj.delete();
        } catch (Exception e) {
            e.printStackTrace();
        }

        logger.info("updateSubject - delete image completed!");
    }

    /**
     * Function Description:
     *
     * @param currentPage
     * @return ModelAndView mv
     * @author zzl
     * @date 2018/10/23
     */
    @RequestMapping(value = "/querySubject")
    public ModelAndView querySubject(HttpServletRequest request, @RequestParam(required = true) int currentPage) {
        logger.info("enterring SubjectMgmtController-querySubject");

        int totalPages = 0;
        totalPages = subjectService.getTotalPages();
        List<Subject> subjectsOfThisPage = subjectService.querySubject(currentPage);

        logger.info("queried subject - " + subjectsOfThisPage);

        ModelAndView mv = new ModelAndView("subjectMgmt");
        mv.addObject("totalPages", totalPages);
        mv.addObject("subjectsOfThisPage", subjectsOfThisPage);

        logger.info("querySubject done!");

        return mv;
    }

    /**
     * Function Description:
     *
     * @param id
     * @return Subject's json representation
     * @author zzl
     * @date 2018/10/23
     */
    @RequestMapping(value = "/querySubjectById")
    @ResponseBody
    public JSONObject querySubjectById(HttpServletRequest request, @RequestParam(required = true) String id) {
        logger.info("enterring SubjectMgmtController-querySubjectById");
        logger.info("id = " + id);
        Subject subject = subjectService.findSubjectById(id);
        logger.info("queried subject - " + subject);

        return (JSONObject) JSON.toJSON(subject);
    }
}
