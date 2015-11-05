/**
 * *************************
 *
 * @Date: Mar 18, 2015
 * @Time: 4:50:02 PM
 * @Author: Rui.Feng
 * <p/>
 * **************************
 */
package cn.beecloud;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;   
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import cn.beecloud.BCEumeration.PAY_CHANNEL;
import cn.beecloud.bean.BCOrderBean;
import cn.beecloud.bean.BCPayParameter;
import cn.beecloud.bean.BCQueryParameter;
import cn.beecloud.bean.BCRefundBean;
import cn.beecloud.bean.BCRefundParameter;
import cn.beecloud.bean.BCRefundQueryParameter;
import cn.beecloud.bean.TransferData;
import net.sf.json.JSONObject;

/**
 * This is the core class of BC payment for external invocation consist of start payment, start refund, start query bill
 * start refund query and check refund status functionality.
 * 
 * @author Ray
 * @since 2015/7/11
 */
public class BCPay {
	
	/**
	 * @param para {@link BCPayParameter}支付参数
	 * (必填)
	 * @return 调起比可支付后的返回结果
	 */
    public static BCPayResult startBCPay(BCPayParameter para) {
    	
    	BCPayResult result;
    	result = ValidationUtil.validateBCPay(para);
    	
    	if (!result.getResultCode().equals("0")) {
    		return result;
    	}
    	
        Map<String, Object> param = new HashMap<String, Object>();
        
        buildPayParam(param, para);
        
        PAY_CHANNEL channel = para.getChannel();
        Client client = BCAPIClient.client;
        WebTarget target = client.target(BCUtilPrivate.getkApiPay());
        try {
            Response response = target.request().post(Entity.entity(param, MediaType.APPLICATION_JSON));
            if (response.getStatus() == 200) {
                Map<String, Object> ret = response.readEntity(Map.class);
                result.setResultCode(ret.get("result_code").toString());
                result.setResultMsg(ret.get("result_msg").toString());
                result.setErrDetail(ret.get("err_detail").toString());

                boolean isSuccess = (result.getResultCode().equals("0"));
                if (isSuccess) {
                	result.setObjectId(ret.get("id").toString());
                	if (channel.equals(PAY_CHANNEL.WX_NATIVE)){
	                    if (ret.containsKey("code_url") && null != ret.get("code_url")) {
	                        result.setCodeUrl(ret.get("code_url").toString());
	                    } 
                	} else if (channel.equals(PAY_CHANNEL.WX_JSAPI)) {
                    	result.setWxJSAPIMap(generateWXJSAPIMap(ret));
                    } else if (channel.equals(PAY_CHANNEL.ALI_WEB) || channel.equals(PAY_CHANNEL.ALI_QRCODE) || channel.equals(PAY_CHANNEL.ALI_WAP)) {
                		if (ret.containsKey("html") && null != ret.get("html") && 
                				ret.containsKey("url") && null != ret.get("url")) {
	                        result.setHtml(ret.get("html").toString());
	                        result.setUrl(ret.get("url").toString());
	                    }
                	} else if (channel.equals(PAY_CHANNEL.UN_WEB) || channel.equals(PAY_CHANNEL.JD_WAP)
                			|| channel.equals(PAY_CHANNEL.JD_WEB) || channel.equals(PAY_CHANNEL.KUAIQIAN_WAP) 
                			|| channel.equals(PAY_CHANNEL.KUAIQIAN_WEB)) {
                		if (ret.containsKey("html") && null != ret.get("html")) {
	                        result.setHtml(ret.get("html").toString());
	                    }
                	} else if (channel.equals(PAY_CHANNEL.YEE_WAP) || channel.equals(PAY_CHANNEL.YEE_WEB) || 
                			channel.equals(PAY_CHANNEL.BD_WEB) || 
                			channel.equals(PAY_CHANNEL.BD_WAP) ) {
                		if (ret.containsKey("url") && null != ret.get("url")) {
	                        result.setUrl(ret.get("url").toString());
	                    }
                	} else if (channel.equals(PAY_CHANNEL.YEE_NOBANKCARD)) {
                		result.setSucessMsg(ValidationUtil.PAY_SUCCESS);
                	}
                } 
            } else {
            	result.setResultCode("0");
            	result.setResultMsg("Not correct response!");
            	result.setErrDetail("Not correct response!");
            }
        } catch (Exception e) {
        	result.setResultCode("-1");
        	result.setResultMsg("Network error!");
        	result.setErrDetail(e.getMessage());
        }
        return result;
    }
    

	/**
	 * @param para {@link BCRefundParameter}退款参数
	 * @return 发起退款的返回结果
	 */
    public static BCPayResult startBCRefund(BCRefundParameter para) {
    	 
    	BCPayResult result;
    	result = ValidationUtil.validateBCRefund(para);
    	
    	if (!result.getResultCode().equals("0")) {
    		return result;
    	}
    	
    	Map<String, Object> param = new HashMap<String, Object>();
    	
    	buildRefundParam(para, param);
         
     	Client client = BCAPIClient.client;

     	WebTarget target = client.target(BCUtilPrivate.getkApiRefund());
     	try {
             Response response = target.request().post(Entity.entity(param, MediaType.APPLICATION_JSON));
             if (response.getStatus() == 200) {
                 Map<String, Object> ret = response.readEntity(Map.class);
                 result.setResultCode(ret.get("result_code").toString());
                 result.setResultMsg(ret.get("result_msg").toString());
                 result.setErrDetail(ret.get("err_detail").toString());

                 boolean isSuccess = (result.getResultCode().equals("0"));

                 if (isSuccess) {
                	 result.setObjectId(ret.get("id").toString());
             		if (ret.containsKey("url")) {
            			result.setUrl(ret.get("url").toString());
            		} 
        			result.setSucessMsg(ValidationUtil.REFUND_SUCCESS);
                 }
             } else {
            	result.setResultCode("0");
             	result.setResultMsg("Not correct response!");
             	result.setErrDetail("Not correct response!");
             }
         } catch (Exception e) {
        	result.setResultCode("-1");
         	result.setResultMsg("Network error!");
         	result.setErrDetail(e.getMessage());
         }
         return result;
    }


    /**
     * @param para {@link BCQueryParameter}订单查询参数
     * @return 订单查询返回的结果
     */
    public static BCQueryResult startQueryBill(BCQueryParameter para) {
    	
    	BCQueryResult result;
    	
    	result = ValidationUtil.validateQueryBill(para);
    	
    	if (!result.getResultCode().equals("0")) {
    		return result;
    	}
    	 
    	Map<String, Object> param = new HashMap<String, Object>();
        buildQueryParam(param, para);
         
        result = new BCQueryResult();
    	
    	Client client = BCAPIClient.client;
    	  
    	StringBuilder sb = new StringBuilder();   
        sb.append(BCUtilPrivate.getkApiQueryBill());
        
        try {
            sb.append(URLEncoder.encode(
                            JSONObject.fromObject(param).toString(), "UTF-8"));

            WebTarget target = client.target(sb.toString());
            Response response = target.request().get();
            if (response.getStatus() == 200) {
                Map<String, Object> ret = response.readEntity(Map.class);
                
                result.setResultCode(ret.get("result_code").toString());
                result.setResultMsg(ret.get("result_msg").toString());
                result.setErrDetail(ret.get("err_detail").toString());

                boolean isSuccess = (result.getResultCode().equals("0"));

                if (isSuccess) {
                    if (ret.containsKey("bills")
                                    && !StrUtil.empty(ret.get("bills"))) {
                        result.setBcOrders(generateBCOrderList((List<Map<String, Object>>)ret.get("bills")));
                    }
                }
            } else {
            	result.setResultCode("0");
            	result.setResultMsg("Not correct response!");
            	result.setErrDetail("Not correct response!");
            }
        } catch (Exception e) {
        	result.setResultCode("-1");
         	result.setResultMsg("Network error!");
         	result.setErrDetail(e.getMessage());
        }
    	return result;
    }
    
    /**
     * Bill Query by Id.
     * @param objectId the id to query by.
     * @return BCQueryResult
     */
    public static BCQueryResult startQueryBillById(String objectId) {
    	
    	 BCQueryResult result;
    	
		 Map<String, Object> param = new HashMap<String, Object>();
	     param.put("app_id", BCCache.getAppID());
	     param.put("timestamp", System.currentTimeMillis());
	     param.put("app_sign", BCUtilPrivate.getAppSignature(param.get("timestamp").toString()));
         
         result = new BCQueryResult();
    	
    	 Client client = BCAPIClient.client;
    	  
    	 StringBuilder sb = new StringBuilder();   
         sb.append(BCUtilPrivate.getkApiQueryBillById());
        
         try {
        	sb.append("/" + URLEncoder.encode(objectId, "UTF-8"));
        	sb.append("?para=");
            sb.append(URLEncoder.encode(
                            JSONObject.fromObject(param).toString(), "UTF-8"));

            WebTarget target = client.target(sb.toString());
            Response response = target.request().get();
            if (response.getStatus() == 200) {
                Map<String, Object> ret = response.readEntity(Map.class);

                result.setResultCode(ret.get("result_code").toString());
                result.setResultMsg(ret.get("result_msg").toString());
                result.setErrDetail(ret.get("err_detail").toString());

                boolean isSuccess = (result.getResultCode().equals("0"));
                if (isSuccess) {
                    if (ret.containsKey("pay")
                                    && ret.get("pay") != null) {
                        result.setOrder(generateBCOrder((Map<String, Object>)ret.get("pay")));
                    }
                }
            } else {
            	result.setResultCode("0");
            	result.setResultMsg("Not correct response!");
            	result.setErrDetail("Not correct response!");
            }
        } catch (Exception e) {
        	result.setResultCode("-1");
         	result.setResultMsg("Network error!");
         	result.setErrDetail(e.getMessage());
        }
    	
    	return result;
    }
    
    /**
     * @param para {@link BCQueryParameter}订单总数查询参数
     * @return 订单总数查询返回的结果
     */
    public static BCQueryResult startQueryBillCount(BCQueryParameter para) {
    	
    	BCQueryResult result;
    	
    	result = ValidationUtil.validateQueryBill(para);
    	
    	if (!result.getResultCode().equals("0")) {
    		return result;
    	}
    	 
    	Map<String, Object> param = new HashMap<String, Object>();
        buildQueryCountParam(param, para);
         
        result = new BCQueryResult();
    	
    	Client client = BCAPIClient.client;
    	  
    	StringBuilder sb = new StringBuilder();   
        sb.append(BCUtilPrivate.getkApiQueryBillCount());
        
        try {
            sb.append(URLEncoder.encode(
                            JSONObject.fromObject(param).toString(), "UTF-8"));

            WebTarget target = client.target(sb.toString());
            Response response = target.request().get();
            if (response.getStatus() == 200) {
                Map<String, Object> ret = response.readEntity(Map.class);

                result.setResultCode(ret.get("result_code").toString());
                result.setResultMsg(ret.get("result_msg").toString());
                result.setErrDetail(ret.get("err_detail").toString());

                boolean isSuccess = (result.getResultCode().equals("0"));

                if (isSuccess) {
                    if (ret.containsKey("count")
                                    && !StrUtil.empty(ret.get("count"))) {
                    	result.setTotalCount((Integer)ret.get("count"));
                    }
                } 
            } else {
            	result.setResultCode("0");
            	result.setResultMsg("Not correct response!");
            	result.setErrDetail("Not correct response!");
            }
        } catch (Exception e) {
        	result.setResultCode("-1");
         	result.setResultMsg("Network error!");
         	result.setErrDetail(e.getMessage());
        }
    	return result;
    }
    
	/**
	 * @param para {@link BCRefundQueryParameter}}
	 * @return 退款查询返回的结果
	 */
    public static BCQueryResult startQueryRefund(BCRefundQueryParameter para) {
    	
    	BCQueryResult result;
    	result = ValidationUtil.validateQueryRefund(para);
    	if (!result.getResultCode().equals("0")) {
    		return result;
    	}
		
		result = new BCQueryResult();
		
		Map<String, Object> param = new HashMap<String, Object>();
        param.put("app_id", BCCache.getAppID());
        param.put("timestamp", System.currentTimeMillis());
        param.put("app_sign", BCUtilPrivate.getAppSignature(param.get("timestamp").toString()));
        buildQueryParam(param, para);
        if (para.getRefundNo() != null) {
        	param.put("refund_no", para.getRefundNo());
        }
        
	    Client client = BCAPIClient.client;
     	
     	StringBuilder sb = new StringBuilder();
     	sb.append(BCUtilPrivate.getkApiQueryRefund());
         
        try {
             sb.append(URLEncoder.encode(
                             JSONObject.fromObject(param).toString(), "UTF-8"));

             WebTarget target = client.target(sb.toString());
             Response response = target.request().get();
             if (response.getStatus() == 200) {
                 Map<String, Object> ret = response.readEntity(Map.class);

                 result.setResultCode(ret.get("result_code").toString());
                 result.setResultMsg(ret.get("result_msg").toString());
                 result.setErrDetail(ret.get("err_detail").toString());

                 boolean isSuccess = (result.getResultCode().equals("0"));
                 if (isSuccess) {
                     if (ret.containsKey("refunds")
                                     && ret.get("refunds") != null) {
                         result.setBcRefundList(generateBCRefundList((List<Map<String, Object>>)ret.get("refunds")));
                     }
                 }
             } else {
            	result.setResultCode("0");
             	result.setResultMsg("Not correct response!");
             	result.setErrDetail("Not correct response!");
             }
         } catch (Exception e) {
        	result.setResultCode("-1");
          	result.setResultMsg("Network error!");
          	result.setErrDetail(e.getMessage());
         }
     	
     	return result;
    }
    
    /**
     * Bill Query by Id.
     * @param objectId the id to query by.
     * @return BCQueryResult
     */
    public static BCQueryResult startQueryRefundById(String objectId) {
    	
    	 BCQueryResult result;
    	
		 Map<String, Object> param = new HashMap<String, Object>();
	     param.put("app_id", BCCache.getAppID());
	     param.put("timestamp", System.currentTimeMillis());
	     param.put("app_sign", BCUtilPrivate.getAppSignature(param.get("timestamp").toString()));
         
         result = new BCQueryResult();
    	
    	 Client client = BCAPIClient.client;
    	  
    	 StringBuilder sb = new StringBuilder();   
         sb.append(BCUtilPrivate.getkApiQueryRefundById());
        
         try {
        	sb.append("/" + URLEncoder.encode(objectId, "UTF-8"));
        	sb.append("?para=");
            sb.append(URLEncoder.encode(
                            JSONObject.fromObject(param).toString(), "UTF-8"));

            WebTarget target = client.target(sb.toString());
            Response response = target.request().get();
            if (response.getStatus() == 200) {
                Map<String, Object> ret = response.readEntity(Map.class);

                result.setResultCode(ret.get("result_code").toString());
                result.setResultMsg(ret.get("result_msg").toString());
                result.setErrDetail(ret.get("err_detail").toString());

                boolean isSuccess = (result.getResultCode().equals("0"));

                if (isSuccess) {
                    if (ret.containsKey("refund")
                                    && ret.get("refund") != null) {
                        result.setRefund(generateBCRefund((Map<String, Object>)ret.get("refund")));
                    }
                }
            } else {
            	result.setResultCode("0");
             	result.setResultMsg("Not correct response!");
             	result.setErrDetail("Not correct response!");
            }
        } catch (Exception e) {
        	result.setResultCode("-1");
          	result.setResultMsg("Network error!");
          	result.setErrDetail(e.getMessage());
        }
    	
    	return result;
    }
    
    /**
     * @param para {@link BCRefundQueryParameter}退款总数查询参数
     * @return 退款总数查询返回的结果
     */
    public static BCQueryResult startQueryRefundCount(BCRefundQueryParameter para) {
    	
    	BCQueryResult result;
    	
    	result = ValidationUtil.validateQueryRefund(para);
    	
    	if (!result.getResultCode().equals("0")) {
    		return result;
    	}
    	 
    	Map<String, Object> param = new HashMap<String, Object>();
        buildQueryCountParam(param, para);
        if (para.getRefundNo() != null) {
        	param.put("refund_no", para.getRefundNo());
        }
         
        result = new BCQueryResult();
    	
    	Client client = BCAPIClient.client;
    	  
    	StringBuilder sb = new StringBuilder();   
        sb.append(BCUtilPrivate.getkApiQueryRefundCount());
        
        try {
            sb.append(URLEncoder.encode(
                            JSONObject.fromObject(param).toString(), "UTF-8"));

            WebTarget target = client.target(sb.toString());
            Response response = target.request().get();
            if (response.getStatus() == 200) {
                Map<String, Object> ret = response.readEntity(Map.class);

                result.setResultCode(ret.get("result_code").toString());
                result.setResultMsg(ret.get("result_msg").toString());
                result.setErrDetail(ret.get("err_detail").toString());

                boolean isSuccess = (result.getResultCode().equals("0"));

                if (isSuccess) {
                    if (ret.containsKey("count")
                                    && !StrUtil.empty(ret.get("count"))) {
                    	result.setTotalCount((Integer)ret.get("count"));
                    }
                } 
            } else {
            	result.setResultCode("0");
             	result.setResultMsg("Not correct response!");
             	result.setErrDetail("Not correct response!");
            }
        } catch (Exception e) {
        	result.setResultCode("-1");
          	result.setResultMsg("Network error!");
          	result.setErrDetail(e.getMessage());
        }
    	return result;
    }
    
    public static BCQueryResult startQueryPrefundByConditon(BCQueryParameter para, String table) {
    	BCQueryResult result;
    	 
    	Map<String, Object> param = new HashMap<String, Object>();
    	param.put("table", table);
    	param.put("appId", BCCache.getAppID());
        param.put("appSign", BCUtilPrivate.getAppSignature());
        List<Map<String, Object>> conditions = new LinkedList<Map<String, Object>>();
        Map<String, Object> channel = new HashMap<String, Object>();
        channel.put("cname", "channel");
        channel.put("value", para.getChannel().toString());
        channel.put("type", "e");
        if (para.getChannel().toString().contains("_")) {
        	channel.put("cname", "sub_channel");
        }
        Map<String, Object> needApproval = new HashMap<String, Object>();
        needApproval.put("cname", "need_approval");
        needApproval.put("value", true);
        needApproval.put("type", "e");
        conditions.add(channel);
        conditions.add(needApproval);
        String order = "createdat,desc";
        param.put("conditions", conditions);
        param.put("order", order);
        param.put("limit", 20);
        
        result = new BCQueryResult();
    	
    	Client client = BCAPIClient.client;
    	  
    	StringBuilder sb = new StringBuilder();   
        sb.append(BCUtilPrivate.getkApiQueryBillByCondition());
        
        try {
            sb.append(URLEncoder.encode(
                            JSONObject.fromObject(param).toString(), "UTF-8"));

            WebTarget target = client.target(sb.toString());
            Response response = target.request().get();
            if (response.getStatus() == 200) {
                Map<String, Object> ret = response.readEntity(Map.class);
                
                result.setResultCode(ret.get("result_code").toString());
                result.setResultMsg(ret.get("result_msg").toString());
                result.setErrDetail(ret.get("err_detail").toString());
                
                boolean isSuccess = (result.getResultCode().equals("0"));

                if (isSuccess) {
                    if (ret.containsKey("results")
                                    && !StrUtil.empty(ret.get("results"))) {
                        result.setBcRefundList(generateBCOrderListByCondition((List<Map<String, Object>>)ret.get("results")));
                    }
                    result.setTotalCount((Integer)ret.get("count"));
                } 
            } else {
            	result.setResultCode("0");
             	result.setResultMsg("Not correct response!");
             	result.setErrDetail("Not correct response!");
            }
        } catch (Exception e) {
        	result.setResultCode("-1");
          	result.setResultMsg("Network error!");
          	result.setErrDetail(e.getMessage());
        }
    	return result;
    }	
    
    /**
     * @param refundNo
     * （必填）商户退款单号， 格式为:退款日期(8位) + 流水号(3~24 位)。不可重复，且退款日期必须是当天日期。流水号可以接受数字或英文字符，建议使用数字，但不可接受“000”。	
     * @param channel
     * (必填) 渠道类型， 根据不同场景选择不同的支付方式，包含：
     *  YEE 易宝
	 *  WX 微信
	 *  KUAIQIAN 快钱
	 *  BD 百度
     * @return BCQueryStatusResult
     */
    public static BCQueryStatusResult startRefundUpdate(PAY_CHANNEL channel, String refundNo) {

    	BCQueryStatusResult result;
    	result = ValidationUtil.validateQueryRefundStatus(channel, refundNo);
    	
		if (!result.getResultCode().equals("0")) {
			return result;
		}
    	
		Map<String, Object> param = new HashMap<String, Object>();
        param.put("app_id", BCCache.getAppID());
        param.put("timestamp", System.currentTimeMillis());
        param.put("app_sign", BCUtilPrivate.getAppSignature(param.get("timestamp").toString()));
        param.put("channel", channel.toString());
        param.put("refund_no", refundNo);
        
        StringBuilder sb = new StringBuilder();   
        sb.append(BCUtilPrivate.getkApiRefundUpdate());
        
        
        Client client = BCAPIClient.client;
        
        try {
        	sb.append(URLEncoder.encode(
                    JSONObject.fromObject(param).toString(), "UTF-8"));
        	WebTarget target = client.target(sb.toString());
		    Response response = target.request().get();
            if (response.getStatus() == 200) {
                Map<String, Object> ret = response.readEntity(Map.class);

                result.setResultCode(ret.get("result_code").toString());
                result.setResultMsg(ret.get("result_msg").toString());
                result.setErrDetail(ret.get("err_detail").toString());

                boolean isSuccess = (result.getResultCode().equals("0"));

                if (isSuccess) {
                	result.setRefundStatus(ret.get("refund_status").toString());
                } 
            } else {
            	result.setResultCode("0");
             	result.setResultMsg("Not correct response!");
             	result.setErrDetail("Not correct response!");
            }
        } catch (Exception e) {
        	result.setResultCode("-1");
          	result.setResultMsg("Network error!");
          	result.setErrDetail(e.getMessage());
        }
        return result;
    	
    }
    
    /**
     * @param channel
     * （必填）渠道类型， 暂时只支持ALI 
     * @param batchNo 
     * （必填） 批量付款批号， 此次批量付款的唯一标示，11-32位数字字母组合
     * @param accountName
     * （必填） 付款方的支付宝账户名, 支付宝账户名称,例如:毛毛
     * @param transferData
     * （必填） 付款的详细数据 {TransferData} 的 List集合。
     * @return BCPayResult
     */
    public static BCPayResult startTransfer(PAY_CHANNEL channel, String batchNo, String accountName, List<TransferData> transferData) {
    	BCPayResult result;
    	result = ValidationUtil.validateBCTransfer(channel, batchNo, accountName, transferData);
    	
    	if (!result.getResultCode().equals("0")) {
    		return result;
    	}
    	
    	Map<String, Object> param = new HashMap<String, Object>();
    	param.put("app_id", BCCache.getAppID());
    	param.put("timestamp", System.currentTimeMillis());
    	param.put("app_sign", BCUtilPrivate.getAppSignature(param.get("timestamp").toString()));
		param.put("channel", channel.toString());
    	param.put("batch_no", batchNo);
    	param.put("account_name", accountName);
    	List<Map<String, Object>> transferList = new ArrayList<Map<String, Object>>();
    	for (TransferData data : transferData) {
    		Map<String, Object> map = new HashMap<String, Object>();
    		map.put("transfer_id", data.getTransferId());
    		map.put("receiver_account", data.getReceiverAccount());
    		map.put("receiver_name", data.getReceiverName());
    		map.put("transfer_fee", data.getTransferFee());
    		map.put("transfer_note", data.getTransferNote());
    		transferList.add(map);
    	}
    	param.put("transfer_data", transferList);
         
     	Client client = BCAPIClient.client;

     	WebTarget target = client.target(BCUtilPrivate.getkApiTransfer());
     	try {
             Response response = target.request().post(Entity.entity(param, MediaType.APPLICATION_JSON));
             if (response.getStatus() == 200) {
                 Map<String, Object> ret = response.readEntity(Map.class);

                 result.setResultCode(ret.get("result_code").toString());
                 result.setResultMsg(ret.get("result_msg").toString());
                 result.setErrDetail(ret.get("err_detail").toString());

                 boolean isSuccess = (result.getResultCode().equals("0"));
                 if (isSuccess) {
        			result.setUrl(ret.get("url").toString());
                 }
             } else {
            	result.setResultCode("0");
              	result.setResultMsg("Not correct response!");
              	result.setErrDetail("Not correct response!");
             }
         } catch (Exception e) {
        	result.setResultCode("-1");
           	result.setResultMsg("Network error!");
           	result.setErrDetail(e.getMessage());
         }
         return result;
    }
    
    /**
     * @param sign
     *            Webhook提供的签名
     * @param timestamp
     *            Webhook提供的timestamp，注意是String格式
     * @return 签名是否正确
     */
    public static boolean verifySign(String sign, String timestamp) {
        String mySign = MD5.sign(BCCache.getAppID() + BCCache.getAppSecret(),
                        timestamp, "UTF-8");
        
        if (sign.equals(mySign))
            return true;
        else
            return false;
    }
    
    /**
     * Build Payment parameters
     * @param param to be built
     * @param para used for building 
     */
    private static void buildPayParam(Map<String, Object> param,
			BCPayParameter para) {
    	
    	param.put("app_id", BCCache.getAppID());
        param.put("timestamp", System.currentTimeMillis());
        param.put("app_sign", BCUtilPrivate.getAppSignature(param.get("timestamp").toString()));
        param.put("channel", para.getChannel().toString());
        param.put("total_fee", para.getTotalFee());
        param.put("bill_no", para.getBillNo());
        param.put("title", para.getTitle());
        
		if (para.getReturnUrl() != null) {
			param.put("return_url", para.getReturnUrl());
		} 
		if (para.getOptional() != null && para.getOptional().size() > 0) {
			param.put("optional", para.getOptional());
		}
		if (para.getOpenId() != null) {
			param.put("openid", para.getOpenId());
		}
		if (para.getShowUrl() != null) {
			param.put("show_url", para.getShowUrl());
		}
		if (para.getQrPayMode() != null) {
			if (para.getQrPayMode().ordinal() == 2) {
        		param.put("qr_pay_mode", String.valueOf(para.getQrPayMode().ordinal() +1));
        	} else {
        		param.put("qr_pay_mode", String.valueOf(para.getQrPayMode().ordinal()));
        	}
		}
		if (para.getBillTimeout() != null) {
        	param.put("bill_timeout", para.getBillTimeout());
        }
        if (para.getChannel().equals(PAY_CHANNEL.YEE_NOBANKCARD)) {
        	param.put("cardno", para.getCardNo());
        	param.put("cardpwd", para.getCardPwd());
        	param.put("frqid", para.getFrqid());
        }
	}
    
    /**
     * Build Refund parameters
     * @param param to be built
     * @param para used for building 
     */
    private static void buildRefundParam(BCRefundParameter para,
			Map<String, Object> param) {
		
		param.put("app_id", BCCache.getAppID());
    	param.put("timestamp", System.currentTimeMillis());
    	param.put("app_sign", BCUtilPrivate.getAppSignature(param.get("timestamp").toString()));
    	param.put("refund_no", para.getRefundNo());
    	param.put("bill_no", para.getBillNo());
    	param.put("refund_fee", para.getRefundFee());
    	
		if (para.getChannel() != null) {
    		param.put("channel", para.getChannel().toString());
    	}
    	if (para.getNeedApproval() != null) {
    		param.put("need_approval", para.getNeedApproval());
    	}
    	if (para.getOptional() != null && para.getOptional().size() > 0)
    		param.put("optional", para.getOptional());
	}
    
    /**
     * Build Query parameters
     * @param param to be built
     * @param para used for building 
     */
	private static void buildQueryParam(Map<String, Object> param,
			BCQueryParameter para) {
		param.put("app_id", BCCache.getAppID());
        param.put("timestamp", System.currentTimeMillis());
        param.put("app_sign", BCUtilPrivate.getAppSignature(param.get("timestamp").toString()));
        if (para.getChannel() != null) {
    		param.put("channel", para.getChannel().toString());
    	}
        if (para.getBillNo() != null) {
        	param.put("bill_no", para.getBillNo());
        }
        if (para.getSkip() != null) {
        	param.put("skip", para.getSkip());
        }
        if (para.getLimit() != null) {
        	param.put("limit", para.getLimit());
        }
        
        if (para.getStartTime() != null) {
        	param.put("start_time", para.getStartTime().getTime());
        }
        if (para.getEndTime() != null) {
       	 	param.put("end_time", para.getEndTime().getTime());
        }
        if (para.getNeedDetail() != null && para.getNeedDetail()) {
        	param.put("need_detail", para.getNeedDetail());
        }
	}
    
	/**
     * Build Query Count parameters
     * @param param to be built
     * @param para used for building 
     */
	private static void buildQueryCountParam(Map<String, Object> param,
			BCQueryParameter para) {
    	param.put("app_id", BCCache.getAppID());
        param.put("timestamp", System.currentTimeMillis());
        param.put("app_sign", BCUtilPrivate.getAppSignature(param.get("timestamp").toString()));
        if (para.getChannel() != null) {
    		param.put("channel", para.getChannel().toString());
    	}
        if (para.getBillNo() != null) {
        	param.put("bill_no", para.getBillNo());
        }
        if (para.getStartTime() != null) {
        	param.put("start_time", para.getStartTime().getTime());
        }
        if (para.getEndTime() != null) {
       	 param.put("end_time", para.getEndTime().getTime());
        }
	}
	
    /**
     * The method is used to generate Order list by query.
     * @param bills
     * @return list of BCOrderBean
     */
	private static List<BCOrderBean> generateBCOrderList(List<Map<String, Object>> bills) {
		
		List<BCOrderBean> bcOrderList = new ArrayList<BCOrderBean>();
		for (Map<String, Object> bill : bills){
			BCOrderBean bcOrder = new BCOrderBean();
			generateBCOrderBean(bill, bcOrder);
			bcOrderList.add(bcOrder);
		}
		return bcOrderList;
	}
    
	/**
     * The method is used to generate an order object.
     * @param bill
     * @return an object of BCOrderBean
     */
	private static BCOrderBean generateBCOrder(Map<String, Object> bill) {
		BCOrderBean bcOrder = new BCOrderBean();
		generateBCOrderBean(bill, bcOrder);
		return bcOrder;
	}
	
    /**
     * The method is used to generate Refund list by query.
     * @param refundList the list of refund taken in
     * @return list of BCRefundBean object
     */
    private static List<BCRefundBean> generateBCRefundList(List<Map<String, Object>> refundList) {
    	
    	List<BCRefundBean> bcRefundList = new ArrayList<BCRefundBean>();
		for (Map<String, Object> refund : refundList){
			BCRefundBean bcRefund = new BCRefundBean();
			generateBCRefundBean(refund, bcRefund);
	    	bcRefundList.add(bcRefund);
		}
		return bcRefundList;
    }
    
    /**
     * The method is used to generate a refund object.
     * @param refund the refund map taken in
     * @return list of BCRefundBean object
     */
    private static BCRefundBean generateBCRefund(Map<String, Object> refund) {
    	BCRefundBean bcRefund = new BCRefundBean();
    	generateBCRefundBean(refund, bcRefund);
    	return bcRefund;
    }
    
    /**
     * Generate order bean from order map
     * @param bill the map taken in
     */
    private static void generateBCOrderBean(Map<String, Object> bill,
			BCOrderBean bcOrder) {
		bcOrder.setBillNo(bill.get("bill_no").toString());
		bcOrder.setTotalFee(bill.get("total_fee").toString());
		bcOrder.setTitle(bill.get("title").toString());
		bcOrder.setChannel(bill.get("channel").toString());
		bcOrder.setSpayResult(((Boolean)bill.get("spay_result")));
		bcOrder.setSubChannel((bill.get("sub_channel").toString()));
		bcOrder.setCreatedTime((Long)bill.get("create_time"));
		if (bill.containsKey("trade_no") && bill.get("trade_no") != null) {
			bcOrder.setChannelTradeNo(bill.get("trade_no").toString());
		}
		bcOrder.setOptional(bill.get("optional").toString());
		bcOrder.setDateTime(BCUtilPrivate.transferDateFromLongToString((Long)bill.get("create_time")));
		if (bill.containsKey("message_detail")) {
			bcOrder.setMessageDetail(bill.get("message_detail").toString());
		}
		bcOrder.setRefundResult((Boolean)bill.get("refund_result"));
		bcOrder.setRevertResult((Boolean)bill.get("revert_result"));
	}
    
    /**
     * Generate refund bean from refund map
     * @param refund the map taken in
     */
	private static void generateBCRefundBean(Map<String, Object> refund,
			BCRefundBean bcRefund) {
		bcRefund.setBillNo(refund.get("bill_no").toString());
		bcRefund.setChannel(refund.get("channel").toString());
		bcRefund.setSubChannel(refund.get("sub_channel").toString());
		bcRefund.setFinished((Boolean)refund.get("finish"));
		bcRefund.setCreatedTime((Long)refund.get("create_time"));
		bcRefund.setOptional(refund.get("optional").toString());
		bcRefund.setRefunded((Boolean)refund.get("result"));
		bcRefund.setTitle(refund.get("title").toString());
		bcRefund.setTotalFee(refund.get("total_fee").toString());
		bcRefund.setRefundFee(refund.get("refund_fee").toString());
		bcRefund.setRefundNo(refund.get("refund_no").toString());
		bcRefund.setDateTime(BCUtilPrivate.transferDateFromLongToString((Long)refund.get("create_time")));
		if (refund.containsKey("message_detail")) {
			bcRefund.setMessageDetail(refund.get("message_detail").toString());
		}
	}
	
	/**
     * Generate a map for JSAPI payment to receive.
     * @param ret
     * @return
     */
    private static Map<String, Object> generateWXJSAPIMap(
			Map<String, Object> ret) {
		HashMap map = new HashMap<String, Object>();
		map.put("appId", ret.get("app_id"));
		map.put("package", ret.get("package"));
		map.put("nonceStr", ret.get("nonce_str"));
		map.put("timeStamp", ret.get("timestamp"));
		map.put("paySign", ret.get("pay_sign"));
		map.put("signType", ret.get("sign_type"));
		
		return map;
	}
}
