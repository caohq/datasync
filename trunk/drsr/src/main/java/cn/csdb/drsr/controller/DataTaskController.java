package cn.csdb.drsr.controller;

import cn.csdb.drsr.model.DataSrc;
import cn.csdb.drsr.model.DataTask;
import cn.csdb.drsr.service.ConfigPropertyService;
import cn.csdb.drsr.service.DataSrcService;
import cn.csdb.drsr.service.DataTaskService;
import cn.csdb.drsr.service.FileResourceService;
import cn.csdb.drsr.utils.PropertiesUtil;
import cn.csdb.drsr.utils.dataSrc.DataSourceFactory;
import cn.csdb.drsr.utils.dataSrc.IDataSource;
import com.alibaba.fastjson.JSONObject;
import org.omg.CORBA.DATA_CONVERSION;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import java.io.File;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Matcher;


/**
 * @program: DataSync
 * @description: 数据任务执行处理控制器
 * @author: xiajl
 * @create: 2018-10-10 10:12
 **/
@Controller
@RequestMapping("/datatask")
public class DataTaskController {
    @Resource
    private DataTaskService dataTaskService;
    @Resource
    private FileResourceService fileResourceService;
    @Resource
    private DataSrcService dataSrcService;
    @Autowired
    private ConfigPropertyService configPropertyService;

    /**
     * Function Description:执行一个数据任务，导出SQL文件后返回执行状态
     *
     * @param:  id: 任务Id
     * @return: 执行结果JsonObject
     * @auther: xiajl
     * @date:   2018/10/18 13:36
     */
    @ResponseBody
    @RequestMapping(value="/{id}")
    public JSONObject executeTask(@PathVariable("id") String id){
        JSONObject jsonObject = new JSONObject();
        DataTask dataTask = dataTaskService.get(Integer.parseInt(id));
        jsonObject = dataTaskService.executeTask(dataTask);
        dataTaskService.packDataResource(jsonObject.get("filePath").toString()+File.separator+dataTask.getDataTaskId()+".zip",Arrays.asList(dataTask.getSqlFilePath().split(";")));
        String fp = jsonObject.get("filePath").toString()+File.separator+dataTask.getDataTaskId()+".zip";
        dataTask.setFilePath(fp.replace(File.separator,"%_%"));
        dataTaskService.update(dataTask);
        return jsonObject;
    }


    /**
     * Function Description: 获取所有的任务列表信息
     *
     * @param:
     * @return: Json 字符串
     * @auther: xiajl
     * @date:   2018/10/18 13:45
     */
    @ResponseBody
    @RequestMapping(value="/getAll")
    public JSONObject getAll(){
        JSONObject jsonObject = new JSONObject();
        List<DataTask> list = dataTaskService.getAllData();
        jsonObject.put("data",list);
        return jsonObject;
    }


    /**
     *
     * Function Description: 数据任务页面跳转
     *
     * @param: []
     * @return: org.springframework.web.servlet.ModelAndView
     * @auther: hw
     * @date: 2018/10/23 14:56
     */
    @RequestMapping("/")
    public ModelAndView datatask() {
        ModelAndView modelAndView = new ModelAndView("datatask");
        return modelAndView;
    }

    /**
     *
     * Function Description: 数据任务展示、查询列表
     *
     * @param: [pageNo, pageSize, datataskType, status]
     * @return: com.alibaba.fastjson.JSONObject
     * @auther: hw
     * @date: 2018/10/24 10:37
     */
    @RequestMapping(value="/list")
    @ResponseBody
    public JSONObject datataskList(@RequestParam(name = "pageNo", defaultValue = "1", required = false) int pageNo,
                                   @RequestParam(name = "pageSize", defaultValue = "10", required = false) int pageSize,
                                   @RequestParam(name = "datataskType", required = false) String datataskType,
                                   @RequestParam(name = "status", required = false) String status){
        JSONObject jsonObject = new JSONObject();
        List<DataTask> dataTasks = dataTaskService.getDatataskByPage((pageNo-1)*pageSize,pageSize,datataskType,status);
        int totalCount = dataTaskService.getCount(datataskType,status);
        jsonObject.put("dataTasks",dataTasks);
        jsonObject.put("totalCount",totalCount);
        jsonObject.put("pageNum",totalCount%pageSize==0?totalCount/pageSize:totalCount/pageSize+1);
        jsonObject.put("pageSize",pageSize);
        return jsonObject;
    }

    /**
     *
     * Function Description:
     *
     * @param: [id]
     * @return: int >0 删除成功 否则失败
     * @auther: hw
     * @date: 2018/10/24 10:47
     */
    @RequestMapping(value="/delete")
    @ResponseBody
    public int deleteDatatask(String datataskId){
        return dataTaskService.deleteDatataskById(Integer.parseInt(datataskId));
    }

    /**
     *
     * Function Description: 查看数据任务信息
     *
     * @param: [id]
     * @return: com.alibaba.fastjson.JSONObject
     * @auther: hw
     * @date: 2018/10/24 10:54
     */
    @RequestMapping(value="detail")
    @ResponseBody
    public JSONObject datataskDetail(String datataskId){
        JSONObject jsonObject = new JSONObject();
        DataTask datatask = dataTaskService.get(Integer.parseInt(datataskId));
        DataSrc dataSrc = dataSrcService.findById(datatask.getDataSourceId());
        jsonObject.put("datatask",datatask);
        jsonObject.put("dataSrc",dataSrc);
        return jsonObject;
    }

    /**
     *
     * Function Description:关系型数据任务保存
     *
     * @param: []
     * @return: com.alibaba.fastjson.JSONObject
     * @auther: hw
     * @date: 2018/10/23 10:29
     */
    @ResponseBody
    @RequestMapping(value="saveRelationDatatask",method = RequestMethod.POST)
    public JSONObject saveRelationDatatask(int dataSourceId,
                                           String datataskName,
                                   String dataRelTableList,
                                   String sqlTableNameEnList,
                                   @RequestParam(name = "dataRelSqlList", required = false)String dataRelSqlList) {
        JSONObject jsonObject = new JSONObject();
        DataTask datatask = new DataTask();
        datatask.setDataSourceId(dataSourceId);
        datatask.setDataTaskName(datataskName);
        datatask.setTableName(dataRelTableList);
        datatask.setSqlString(dataRelSqlList);
        datatask.setSqlTableNameEn(sqlTableNameEnList);
        datatask.setCreateTime(new Date());
        datatask.setDataTaskType("mysql");
        datatask.setStatus("0");
        int flag = dataTaskService.insertDatatask(datatask);
        jsonObject.put("result",flag);
        if(flag < 0){
            return  jsonObject;
        }
        return jsonObject;
    }

    @ResponseBody
    @RequestMapping(value="saveFileDatatask",method = RequestMethod.POST)
    public JSONObject saveFileDatatask(int dataSourceId, String datataskName,String[] nodes){

        JSONObject jsonObject = new JSONObject();
        DataTask datatask = new DataTask();
        datatask.setDataSourceId(dataSourceId);
        StringBuffer filePath = new StringBuffer("");
        for (String nodeId : nodes){
            String str = nodeId.replaceAll("%_%", Matcher.quoteReplacement(File.separator));
            String str1 = fileResourceService.traversingFiles(str);
            filePath.append(str1);
        }
        datatask.setFilePath(filePath.toString());
        datatask.setDataTaskName(datataskName);
        datatask.setCreateTime(new Date());
        datatask.setDataTaskType("file");
        datatask.setStatus("0");
        int datataskId = dataTaskService.insertDatatask(datatask);
        if(dataSourceId <0 ){
            jsonObject.put("result",false);
            return  jsonObject;
        }
        List<String> filepaths = Arrays.asList(filePath.toString().split(";"));
        String subjectCode = configPropertyService.getProperty("SubjectCode");
        String fileName = subjectCode+"_"+datataskId;
        fileResourceService.packDataResource(fileName,filepaths);
        String zipFile = System.getProperty("drsr.framework.root") + "zipFile" + File.separator + fileName + ".zip";
        DataTask dt = dataTaskService.get(datataskId);
        dt.setSqlFilePath(zipFile.replace(File.separator,"%_%"));
        boolean upresult = dataTaskService.update(dt);
        jsonObject.put("result",true);
        return  jsonObject;
    }
}
