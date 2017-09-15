package com.sgm.wms.core.inbound.linebyreceive.impl;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Resource;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.CommonsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.web.client.RestTemplate;

import com.sgm.framework.common.exception.AppException;
import com.sgm.framework.common.usermgnt.domain.User;
import com.sgm.framework.component.orm.hibernate.Sequence;
import com.sgm.framework.component.session.SessionManager;
import com.sgm.framework.util.DateUtils;
import com.sgm.framework.util.ResourceUtils;
import com.sgm.wms.constant.Constants;
import com.sgm.wms.core.inbound.linebyreceive.LinebyReceiveService;
import com.sgm.wms.inf.boldetail.dao.BolDetailDao;
import com.sgm.wms.map.billmapmelos.dto.BillMapMelosDto;
import com.sgm.wms.map.billmapmelos.service.BillMapMelosService;
import com.sgm.wms.map.melosemapwms.dto.MeloseMapWmsDto;
import com.sgm.wms.map.melosemapwms.service.MeloseMapWmsService;
import com.sgm.wms.map.melosshipping.dao.MelosShippingDao;
import com.sgm.wms.map.melosshipping.domain.MelosShipping;
import com.sgm.wms.map.melosshipping.service.MelosShippingService;
import com.sgm.wms.map.movemapmelose.service.MoveMapMeloseService;
import com.sgm.wms.wm.common.idgenerator.NumberRangeConstant;
import com.sgm.wms.wm.common.idgenerator.NumberRangeGenService;
import com.sgm.wms.wm.core.constant.InventoryTransactionStatus;
import com.sgm.wms.wm.core.constant.InventoryTransactionTypesConstant;
import com.sgm.wms.wm.core.domain.InventoryTransation;
import com.sgm.wms.wm.core.domain.InventoryTransationId;
import com.sgm.wms.wm.core.service.InventoryTransactionService;
import com.sgm.wms.wm.inbound.asndetail.dto.AsnDetailDto;
import com.sgm.wms.wm.inbound.asnheader.dto.AsnHeaderDto;
import com.sgm.wms.wm.inbound.asnheader.service.AsnHeaderService;
import com.sgm.wms.wm.outbound.bol.dto.BolDetailDto;
import com.sgm.wms.wm.outbound.bol.dto.BolHeaderDto;
import com.sgm.wms.wm.outbound.so.dao.SODao;
import com.sgm.wms.wm.outbound.so.dao.SODetailDao;
import com.sgm.wms.wm.outbound.so.domain.SO;
import com.sgm.wms.wm.outbound.so.domain.SODetail;
import com.sgm.wms.wm.outbound.so.domain.SODetailId;
import com.sgm.wms.wm.outbound.so.domain.SOId;
import com.sgm.wms.wm.outbound.so.dto.SODetailDto;
import com.sgm.wms.wm.outbound.so.dto.SODto;
import com.sgm.wms.wm.util.BeanUtils;
import com.sgm.wms.wm.warn.domain.Warn;
import com.sgm.www.wms.EpsNoOrderService.ENOSOperation;

@Service("linebyReceiveService")
public class LinebyReceiveServiceImpl implements LinebyReceiveService {
	
	private final static Logger logger = LoggerFactory
			.getLogger(LinebyReceiveServiceImpl.class);
	
	@Resource(name = "infBolDetailDao")
	BolDetailDao bolDetailDao;
	
	@Resource(name = "numberRangeGenService")
	private NumberRangeGenService numberRangeGenService;
	
	@Resource(name = "melosShippingService")
	private MelosShippingService melosShippingService;
	
	@Resource(name = "meloseMapWmsService")
	private MeloseMapWmsService meloseMapWmsService;
	
	@Resource(name = "billMapMelosService")
	private BillMapMelosService billMapMelosService;
	
	@Resource(name = "moveMapMeloseService")
	private MoveMapMeloseService moveMapMeloseService;
	
	@Resource(name = "soDao")
	private SODao soDao;
	
	@Resource(name = "soDetailDao")
	private SODetailDao soDetailDao;
	
	@Resource(name = "inventoryTransactionService")
	private InventoryTransactionService inventoryTransactionService;
	
	@Resource(name = "asnHeaderService")
	private AsnHeaderService asnHeaderService;
	
	@Resource(name = "melosShippingDao")
	private MelosShippingDao melosShippingDao;

	@Override
	@Transactional(rollbackFor = { Exception.class })
	public void genLinebyReceiveOrder(List<BolHeaderDto> bolHeaderDtoList) {
		// TODO Auto-generated method stub
		try{
			User user = SessionManager.getUser();
			List<BolDetailDto> bolDetailDtoList = new ArrayList<BolDetailDto>();
			for (BolHeaderDto bolHeaderDto : bolHeaderDtoList) {
				bolDetailDtoList.addAll(bolHeaderDto.getDetails());
			}
			Set<Map<String, Object>> bolGroupSet = new HashSet<Map<String, Object>>();
			for(BolDetailDto bolDetailDto:bolDetailDtoList){
				Map<String, Object> bolGroupMap = new HashMap<String, Object>();
				bolGroupMap.put("bolNo", bolDetailDto.getBolNo());
				bolGroupMap.put("warehouseId", bolDetailDto.getWarehouseId());
				bolGroupSet.add(bolGroupMap);
			}
			for(Map<String, Object> bolGroupMap:bolGroupSet){
				List<BolDetailDto> listBolDetailDto = this.bolDetailDao.queryForList("BolDetail.groupBDByBolNoAndExternalNo", bolGroupMap);
				for(BolDetailDto bolDetailDto : listBolDetailDto){
					String asnNo = numberRangeGenService.genNextNumberByWarehouseId(bolGroupMap.get("warehouseId").toString(),
							NumberRangeConstant.ASN);
					Map<String, Object> param = new HashMap<String,Object>();
					param.put("asnNo", asnNo);
					param.put("billType", ResourceUtils.getCodeInfo("ASN_TYPE","13"));
					param.put("billStatus", "0");
					if(user != null){
						param.put("createBy", user.getId());
						param.put("lastUpdateBy", user.getId());
					}
					else{
						param.put("createBy", 1L);
						param.put("lastUpdateBy", 1L);
					}
					param.put("bolNo", bolDetailDto.getBolNo());
					param.put("externalNo", bolDetailDto.getExternalNo());
					param.put("receiptStatus", ResourceUtils.getCodeInfo("ASN_ORDER_STATUS","ASN_STATUS_NEW"));
					param.put("soBillType", ResourceUtils.getCodeInfo("SO_BILL_TYPE","BILL_TYPE_PULLOD"));
					this.bolDetailDao.executeUpdateWithNamedSQL("BolDetail.insertLineByReceiveOrder", param);
					TransactionAspectSupport.currentTransactionStatus().flush();
				}
			}
		}
		catch (Exception e) {
			// TODO: handle exception
//			e.printStackTrace();
			logger.error(e.getMessage());
		}
		
//		return null;
	}

	@Override
	@Transactional(rollbackFor = { Exception.class })
	public void createShippingInfo(AsnHeaderDto asnHeaderDto) {
		// TODO Auto-generated method stub
		List<AsnDetailDto> listAsnDetail = asnHeaderDto.getAsnDetails();
		try {
//			long i = 1L;
//			List<MelosShipping> melosShippings = new ArrayList<MelosShipping>();
			Map<String,Object> parameter=new HashMap<String, Object>();
			parameter.put("len", 1);
			Sequence<Integer> seq =  this.soDao.queryForSequence("SEQ_TI_MELOS_SHIPPING_TID", parameter);
			Long tid=Long.valueOf(seq.next());
			for(AsnDetailDto asnDetailDto:listAsnDetail){
				MelosShipping melosShipping = new MelosShipping();
				melosShipping.setStatus(ResourceUtils.getCodeInfo("INTERFACE_UP_STATUS","0"));
				melosShipping.setCreateBy(asnHeaderDto.getCreateBy());
				melosShipping.setCreateDate(new Date());
				melosShipping.setLastUpdateBy(asnHeaderDto.getLastUpdateBy());
				melosShipping.setLastUpdateDate(new Date());
				melosShipping.setExternalSystem(ResourceUtils.getCodeInfo("BILL_SOURCE","BILLSOURCE_WMS"));
				melosShipping.setExternalNo(asnHeaderDto.getExternalBillNo());
				melosShipping.setHuId(asnDetailDto.getHuId());
				Map<String, Object> param = new HashMap<String, Object>();
				param.put("wmsPlantId", asnHeaderDto.getPlantId());
				param.put("externalSystem", ResourceUtils.getCodeInfo("BILL_SOURCE","BILLSOURCE_MELOS"));
				
				List<MeloseMapWmsDto> listMelosMapWms = this.meloseMapWmsService.findMeloseMapWmsByMap(param);
				if(listMelosMapWms.size() > 0){
					MeloseMapWmsDto melosMapWmsDto = listMelosMapWms.get(0);
					melosShipping.setMelosPlantId(melosMapWmsDto.getMelosPlantId());
				}
				param.put("sourcePlant", asnHeaderDto.getPlantId());
				param.put("wmsBillType", ResourceUtils.getCodeInfo("SO_BILL_TYPE","BILL_TYPE_PULLOD"));
				param.put("wmsBizType", ResourceUtils.getCodeInfo("SO_BIZ_TYPE","BIZ_TYPE_PPS"));
				param.put("externalSystem", ResourceUtils.getCodeInfo("BILL_SOURCE","BILLSOURCE_MELOS"));
				param.put("inoutSign", ResourceUtils.getCodeInfo("INOUT_SIGN","OUT"));
				List<BillMapMelosDto> listBillMapMelosDto = this.billMapMelosService.queryBillMapMelosList(param);
				if(listBillMapMelosDto.size() > 0){
					BillMapMelosDto billMapMelosDto = listBillMapMelosDto.get(0);
					melosShipping.setMelosBillType(billMapMelosDto.getMelosBillType());
				}
//				param.put("targetLocation", "MB40");
//				param.put("warehouseId", asnHeaderDto.getWarehouseId());
//				this.moveMapMeloseService.queryMoveMapMeloseList(param);
				melosShipping.setMelosMoveType("DD01");
				melosShipping.setTransactionId(DateUtils.format(new Date(), Constants.DATE_FORMAT_PATTERNYMD)+String.format("%06d", tid)+"_"
						+melosShipping.getMelosBillType()+"_"+melosShipping.getMelosPlantId());
				melosShipping.setPartId(asnDetailDto.getPartId());
				melosShipping.setShipQty(asnDetailDto.getReceiptQty());
				melosShipping.setItemNo(Long.valueOf(asnDetailDto.getExternalLineNo()));
//				i++;
				this.melosShippingService.addMelosShipping(melosShipping);
//				melosShippings.add(melosShipping);
			}
//			this.melosShippingDao.batchSave(melosShippings);
		} catch (AppException e) {
			// TODO Auto-generated catch block
			logger.error(e.getMessage());
		}
	}

	@Override
	@Transactional(rollbackFor = { Exception.class })
	public void updateSo(AsnHeaderDto asnHeaderDto) {
		// TODO Auto-generated method stub
		List<AsnDetailDto> listAsnDetail = asnHeaderDto.getAsnDetails();
		try {
			for(AsnDetailDto asnDetailDto:listAsnDetail){
				Map<String, Object> parameterMap = new HashMap<String, Object>();
				parameterMap.put("warehouseId", asnDetailDto.getWarehouseId());
				parameterMap.put("huId", asnDetailDto.getHuId());
				parameterMap.put("billType", ResourceUtils.getCodeInfo("SO_BILL_TYPE","BILL_TYPE_NOOD"));
				List<SODto> soDtoList = this.soDao.queryForList("SO.querySoByHuId", parameterMap);
				for(SODto soDto:soDtoList){
					SO so = new SO();
					BeanUtils.copyProperties(soDto, so,false);
					if(asnDetailDto.getExpectQty().compareTo(asnDetailDto.getReceiptQty()) != 0){
						so.setReceiptStatus(ResourceUtils.getCodeInfo("SO_RECEIVE_STATUS","SO_STATUS_DIFFRECEIVE"));
					}
					else{
						so.setReceiptStatus(ResourceUtils.getCodeInfo("SO_RECEIVE_STATUS","SO_STATUS_NORMALRECEIVE"));
					}
					SOId soId = new SOId();
					soId.setSoNo(soDto.getSoNo());
					soId.setWarehouseId(soDto.getWarehouseId());
					so.setSoId(soId);
					this.soDao.update(so);
				}
				List<SODetailDto> soDetailList = this.soDetailDao.queryForList("SODetail.querySoDetailByHuId", parameterMap);
				for(SODetailDto soDetailDto:soDetailList){
					SODetail soDetail = new SODetail();
					BeanUtils.copyProperties(soDetailDto, soDetail,false);
					if(asnDetailDto.getExpectQty().compareTo(asnDetailDto.getReceiptQty()) != 0){
						soDetail.setReceiptStatus(ResourceUtils.getCodeInfo("SO_RECEIVE_STATUS","SO_STATUS_DIFFRECEIVE"));
					}
					else{
						soDetail.setReceiptStatus(ResourceUtils.getCodeInfo("SO_RECEIVE_STATUS","SO_STATUS_NORMALRECEIVE"));
					}
					soDetail.setReceiptQty(asnDetailDto.getReceiptQty());
					SODetailId soDetailId = new SODetailId();
					soDetailId.setSoNo(soDetailDto.getSoNo());
					soDetailId.setSoLineNo(soDetailDto.getSoLineNo());
					soDetailId.setWarehouseId(soDetailDto.getWarehouseId());
					soDetail.setSoDetailId(soDetailId);
					this.soDetailDao.update(soDetail);
				}
			}
		}catch (Exception e) {
			// TODO Auto-generated catch block
			logger.error(e.getMessage());
		}
	}

	@Override
	@Transactional(rollbackFor = { Exception.class })
	public void confirmReceive(AsnHeaderDto asnHeaderDto) throws AppException{
		// TODO Auto-generated method stub
		this.asnHeaderService.addAsnHeaderAndDetails(asnHeaderDto);
		List<InventoryTransation> itList = getInventoryTransation(asnHeaderDto.getAsnDetails());
		for(InventoryTransation inventoryTransation:itList){
			inventoryTransactionService.createInvTran(inventoryTransation);
		}
		this.createShippingInfo(asnHeaderDto);
		this.updateSo(asnHeaderDto);
	}
	
	
	
	@SuppressWarnings("rawtypes")
	@Override
	public Map callMelosCheckTask(ENOSOperation enosOperation) throws AppException {
		// TODO Auto-generated method stub
		String url = ResourceUtils.getCodeInfo("WS_INTERFACE_URL_TYPE", "MELOS_NOORDER_URL_CODE");
		String username = ResourceUtils.getCodeInfo("WS_INTERFACE_URL_TYPE", "MELOS_USERNAME");
		String password = ResourceUtils.getCodeInfo("WS_INTERFACE_URL_TYPE", "MELOS_PASSWORD");
		HttpClient client = new HttpClient();
		client.getParams().setAuthenticationPreemptive(true);
		Credentials defaultcreds = new UsernamePasswordCredentials(username, password);
		client.getState().setCredentials(AuthScope.ANY, defaultcreds);
		RestTemplate restTemplate = new RestTemplate();
		restTemplate.setRequestFactory(new CommonsClientHttpRequestFactory(client));
		Map<String, Object> para = new HashMap<String, Object>();
		para.put("wmsPlantId", enosOperation.getPlantId());
		para.put("externalSystem", ResourceUtils.getCodeInfo("BILL_SOURCE","BILLSOURCE_MELOS"));
		
		List<MeloseMapWmsDto> listMelosMapWms = this.meloseMapWmsService.findMeloseMapWmsByMap(para);
		if(listMelosMapWms.size() > 0){
			Map<String, Object> param = new HashMap<String, Object>();
			param.put("plantCode", listMelosMapWms.get(0).getMelosPlantId());
			param.put("uloc", enosOperation.getWorkNo());
			param.put("partCode", enosOperation.getPartId());
			param.put("huId", enosOperation.getHuId());
			HttpHeaders headers = new HttpHeaders();
			headers.add("Accept", "application/json");
			headers.add("Content-Type", "application/json;charset=UTF-8");
			headers.add("X-SGM-MSG-ID", UUID.randomUUID().toString());
			headers.add("X-SGM-FROM-SYS-ID", "WMS");
			headers.add("X-SGM-TO-SYS-ID", "MELOS");
			headers.add("X-SGM-MSG-TS", DateUtils.format(new Date(), 
					Constants.DEFAULT_DATE_FORMAT_PATTERN).replaceAll(" ", "T"));
			HttpEntity<Map> entity = new HttpEntity<Map>(param,headers);
			
			ResponseEntity<Map> response = restTemplate.postForEntity(
					url, 
					entity, Map.class);
			
			Map result = response.getBody();
			if(result != null){
				return result;
			}
			else {
				return result;
			}
		}
		else{
			return null;
		}
	}

	private List<InventoryTransation> getInventoryTransation(List<AsnDetailDto> listAsnDetail) throws AppException {

		List<InventoryTransation> list = new ArrayList<InventoryTransation>();
		for(AsnDetailDto asnDetailDto:listAsnDetail){
			InventoryTransation it = new InventoryTransation();

			it.setMemo("");
			it.setFromBinId(asnDetailDto.getFromBin());
			it.setFromHuId(asnDetailDto.getHuId());
			it.setScanFromHuId(asnDetailDto.getHuId());
			it.setFromPlantId(asnDetailDto.getPlantId());
			it.setFromStockStatus(asnDetailDto.getStockStatus());
			it.setFromStockType(asnDetailDto.getStockType());
			it.setFromLot(asnDetailDto.getLot());

			it.setToBinId(asnDetailDto.getReceiveBin());
			it.setToHuId(asnDetailDto.getHuId());
			it.setScanToHuId(asnDetailDto.getHuId());
			it.setToPlantId(asnDetailDto.getPlantId());
			it.setToStockStatus(asnDetailDto.getStockStatus());
			it.setToStockType(asnDetailDto.getStockType());
			it.setToLot(asnDetailDto.getLot());

			it.setPartId(asnDetailDto.getPartId());
			it.setTargetQty(asnDetailDto.getReceiptQty());
			it.setActualQty(asnDetailDto.getReceiptQty());

			it.setCreateBy(asnDetailDto.getCreateBy());
			it.setLastUpdateBy(asnDetailDto.getCreateBy());

			it.setStatus(InventoryTransactionStatus.getValue("E"));
			it.setInvTranType(InventoryTransactionTypesConstant.INV_TRAN_TYPE_CHANGE_INVNETORY);
			
			// 关联单号,关联号,业务类型
			it.setReferenceNo(asnDetailDto.getAsnNo());
			it.setReferenceOrderNo(asnDetailDto.getAsnNo());
			it.setReferenceOrderLineNo(String.valueOf(asnDetailDto.getAsnLineNo()));
			it.setReferenceOrderType("ASN");
			it.setMoveType(ResourceUtils.getCodeInfo("WM_OPERATION","LINEBY_RECEIVE"));

			InventoryTransationId itId = new InventoryTransationId();
			itId.setWarehouseId(asnDetailDto.getWarehouseId());
			it.setId(itId);

			list.add(it);
		}
		

		return list;
	}
	
}
