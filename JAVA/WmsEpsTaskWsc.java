package com.sgm.melos.eps.ws.impl;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.sgm.melos.constant.BusinessExceptionConstant;
import com.sgm.melos.data.constants.AppConstants;
import com.sgm.melos.data.dao.ITaskDao;
import com.sgm.melos.data.dto.TaskDto;
import com.sgm.melos.exception.BusinessException;
import com.sgm.melos.util.MonitorUtil;

@Controller
@RequestMapping("/wmsEpsTask")
public class WmsEpsTaskWsc {

	private static final Logger logger = LoggerFactory.getLogger(WmsEpsTaskWsc.class);

	@Autowired
	private ITaskDao taskDao;

	@Autowired
	private EpsTerminalTransactionWsc epsTerminalTransactionWsc;

	@RequestMapping(value = "/checkWmsEpsTask", method = RequestMethod.POST)
	public @ResponseBody TaskResult checkWmsEpsTask(@RequestParam String plantCode, @RequestParam String uloc,
			@RequestParam String partCode, @RequestParam String huId) {
		List<TaskDto> taskList = taskDao.searchEpsTask(plantCode, uloc, partCode);
		if (taskList.size() > 0) {

		}
		TaskResult taskResult = new TaskResult();
		taskResult.setPartCode(partCode);
		taskResult.setTaskStatus("0");
		taskResult.setTaskNo("test");
		taskResult.setRemark("test");
		return taskResult;
	}

	@RequestMapping(value = "/checkWmsEpsTaskN", method = RequestMethod.POST)
	public @ResponseBody TaskResult checkWmsEpsTaskN(@RequestBody Task task) throws BusinessException {
		try {
			TaskResult taskResult = null;
			List<TaskDto> taskList = taskDao.searchEpsTask(task.getPlantCode(), task.getUloc(), task.getPartCode());
			if (taskList.size() > 0) {
				for (TaskDto taskDto : taskList) {
					if (taskDto.getSapCalFlag().equals("1")) {
						taskResult = new TaskResult();
						taskResult.setPartCode(task.getPartCode());
						taskResult.setTaskStatus("3");
						taskResult.setTaskNo(taskDto.getTaskId());
						taskResult.setRemark("不允许WMS接收");
						break;
					}
					else {
						break;
					}
				}
				if (taskResult == null || !taskResult.getTaskStatus().equals("3")) {
					for (TaskDto taskDto : taskList) {
						if (taskDto.getUloc().equals(task.getUloc()) && taskDto.getTaskStatus().equals("3")) {
							//不等于0和2填FALSE,否则添TRUE
							if(taskDto.getPullType().equals(AppConstants.PARTPULL_PULLTYPE_NOT_PULL)||
									taskDto.getPullType().equals(AppConstants.PARTPULL_PULLTYPE_PULL_EPS)){
								epsTerminalTransactionWsc.completeTask(Integer.valueOf(taskDto.getSessionId()),
										Integer.valueOf(taskDto.getTaskId()), 
										true);
							}
							else{
								epsTerminalTransactionWsc.completeTask(Integer.valueOf(taskDto.getSessionId()),
										 Integer.valueOf(taskDto.getTaskId()), false);
							}
							taskResult = new TaskResult();
							taskResult.setPartCode(task.getPartCode());
							taskResult.setTaskStatus("1");
							taskResult.setTaskNo(taskDto.getTaskId());
							taskResult.setRemark("当前工位存在已认领任务");
							break;
						}
						else{
							break;
						}
					}
				}
				if (taskResult == null || (!taskResult.getTaskStatus().equals("1") && !taskResult.getTaskStatus().equals("3"))) {
					taskResult = new TaskResult();
					taskResult.setPartCode(task.getPartCode());
					taskResult.setTaskStatus("2");
					taskResult.setTaskNo(taskList.get(0).getTaskId());
					for (TaskDto taskDto : taskList) {
						if(taskResult.getRemark() == null || taskResult.getRemark().equals("")){
							taskResult.setRemark(taskDto.getUloc() + ";");
							continue;
						}
						if (!taskDto.getUloc().equals(task.getUloc()) && !taskResult.getRemark().contains(taskDto.getUloc())) {
							taskResult.setRemark(taskDto.getUloc() + ";" + taskResult.getRemark());
//							break;
						}
					}
				}
				return taskResult;
			} else {
				taskResult = new TaskResult();
				taskResult.setPartCode(task.getPartCode());
				taskResult.setTaskStatus("0");
				taskResult.setTaskNo("000000");
				taskResult.setRemark("不存在任务");
				return taskResult;
			}
		
//		} catch (BusinessException e) {
//			throw e;
		} catch (Exception e) {
			logger.error("method checkWmsEpsTaskN error, ", e);
			throw new BusinessException(BusinessExceptionConstant.ERR_10CB4, e);
		}
	}
}
