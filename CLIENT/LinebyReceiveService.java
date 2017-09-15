package com.sgm.wms.core.inbound.linebyreceive;

import java.util.List;
import java.util.Map;

import com.sgm.framework.common.exception.AppException;
import com.sgm.wms.wm.inbound.asnheader.dto.AsnHeaderDto;
import com.sgm.wms.wm.outbound.bol.dto.BolHeaderDto;
import com.sgm.www.wms.EpsNoOrderService.ENOSOperation;

public interface LinebyReceiveService {
	
	/**
	 * 生成线旁收货单
	 * @param bolHeaderDtoList
	 * @Date 2017-07-10
	 */
	public void genLinebyReceiveOrder(List<BolHeaderDto> bolHeaderDtoList);
	
	
	/**
	 * 创建扣账信息
	 * @param asnHeaderDto
	 * @Date 2017-07-10
	 */
	public void createShippingInfo(AsnHeaderDto asnHeaderDto);

	/**
	 * 更新出库单状态
	 * @param asnHeaderDto
	 * @Date 2017-07-10
	 */
	public void updateSo(AsnHeaderDto asnHeaderDto);
	
	
	/**
	 * 确认收货
	 * @param asnHeaderDto
	 * @throws AppException
	 * @Date 2017-07-10
	 */
	public void confirmReceive(AsnHeaderDto asnHeaderDto) throws AppException;
	
	/**
	 * 调用MELOS接口检查EPS任务
	 * @param enosOperation
	 * @return
	 * @throws AppException
	 * @Date 2017-08-31
	 */
	@SuppressWarnings("rawtypes")
	public Map callMelosCheckTask(ENOSOperation enosOperation) throws AppException;
}
