package com.polypay.platform.paychannel;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Maps;
import com.polypay.platform.bean.MerchantPlaceOrder;
import com.polypay.platform.bean.MerchantSettleOrder;
import com.polypay.platform.utils.DateUtils;
import com.polypay.platform.utils.HttpClientUtil;
import com.polypay.platform.utils.HttpRequestDetailVo;
import com.polypay.platform.utils.MD5;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;

/**
 * create by wenjie.mao
 * Date: 2019/11/7
 */
public class YSBChannel implements IPayChannel {

    private static final String MERCHANT_NO = "2120191104170809001";

    private static final String MERCHANT_KEY = "howe528741";

    private static final String PAY_URL = "https://unspay.com/unspay/page/linkbank/payRequest.do";


    @Override
    public void sendRedirect(Map<String, Object> param, HttpServletResponse response, HttpServletRequest request) {


        Map<String, String> sendParam = Maps.newHashMap();

        String version = request.getParameter("version");
        if (version == null || version.length() == 0) version = "4.0.0"; //接口版本
        sendParam.put("version", version);

        String requestUrl = request.getParameter("requestUrl");
        if (requestUrl == null || requestUrl.length() == 0) requestUrl = PAY_URL;
        //sendParam.put("requestUrl", requestUrl);

        String merchantId = request.getParameter("merchantId");
        if (merchantId == null) merchantId = MERCHANT_NO; //注册商户在银生宝的客户编号
        sendParam.put("merchantId", merchantId);

        String merchantKey = request.getParameter("merchantKey");
        if (merchantKey == null) merchantKey = MERCHANT_KEY; //注册商户在银生宝设置的密钥
        sendParam.put("merchantKey", merchantKey);

        String merchantUrl = request.getParameter("merchantUrl");
        if (merchantUrl == null || merchantUrl.length() == 0)
            merchantUrl = "http://localhost/merchant/mockMerchantResponse.jsp"; //商户反馈url
        sendParam.put("merchantUrl", merchantUrl);

        String responseMode = request.getParameter("responseMode");
        if (responseMode == null) responseMode = "1"; //响应方式，1-页面响应，2-后台响应，3-两者都需
        sendParam.put("responseMode", responseMode);

        String assuredPay = request.getParameter("assuredPay"); //是否担保支
        if (assuredPay == null) assuredPay = "";
        sendParam.put("assuredPay", assuredPay);

        String cardAssured = request.getParameter("cardAssured");
        if (cardAssured == null) cardAssured = "0";
        sendParam.put("cardAssured", cardAssured);
//订单的生成
        String time = DateUtils.getOrderTime();
        sendParam.put("time", time);

        String orderId = param.get("we_order_number").toString();//订单id[商户网站]
        sendParam.put("orderId", orderId);

        String currencyType = "CNY"; //货币种类，暂时只支持人民币：CNY
        sendParam.put("currencyType", currencyType);

        String amount = param.get("pay_amount").toString();
        if (amount == null) amount = "1"; //支付金额
        sendParam.put("amount", amount);

        String remark = request.getParameter("remark");
        if (remark == null) remark = ""; //备注，附加信息
        sendParam.put("remark", remark);

        String bankCode = request.getParameter("bankCode"); //银行代码或者商联卡支付或者银生宝账户支付
        if (bankCode == null) bankCode = "";
        sendParam.put("bankCode", bankCode);

        boolean b2b = Boolean.valueOf(request.getParameter("b2b")).booleanValue();
        ; //是否B2B支付
        sendParam.put("b2b", Boolean.TRUE.toString());

        // C 贷 D 借
        String bankCardType = "D";
        String commodityName = "投资管理";
        String businessTypeNO = "130001";

        String commodity = request.getParameter("commodity"); //商品名称
        sendParam.put("commodity", commodity);

        String orderUrl = request.getParameter("orderUrl"); //订单url
        sendParam.put("orderUrl", orderUrl);

        sendParam.put("bankCardType", bankCardType);
        sendParam.put("commodityName", commodityName);
        sendParam.put("businessTypeNO", businessTypeNO);

        StringBuffer s = new StringBuffer();
        s.append("merchantId=").append(merchantId);
        s.append("&merchantUrl=").append(merchantUrl);
        s.append("&responseMode=").append(responseMode);
        s.append("&orderId=").append(orderId);
        s.append("&currencyType=").append(currencyType);
        s.append("&amount=").append(amount);
        s.append("&assuredPay=").append(assuredPay);
        s.append("&time=").append(time);
        s.append("&remark=").append(remark);
        s.append("&merchantKey=").append(merchantKey);
//md5加密
        String mac = MD5.md5(s.toString()).toUpperCase();
        sendParam.put("mac", mac);
        request.setAttribute("action", PAY_URL);
        request.setAttribute("dataMap", sendParam);

    }

    @Override
    public Map<String, Object> checkOrder(HttpServletRequest request) {

        String merchantId = request.getParameter("merchantId");
        String merchantKey = MERCHANT_KEY;
        String responseMode = request.getParameter("responseMode");
        String orderId = request.getParameter("orderId");
        String currencyType = request.getParameter("currencyType");
        String amount = request.getParameter("amount");
        String returnCode = request.getParameter("returnCode");
        String returnMessage = request.getParameter("returnMessage");
        String mac = request.getParameter("mac");
        boolean success = "0000".equals(returnCode);
        boolean paid = "0001".equals(returnCode);
        StringBuffer s = new StringBuffer(50);
        //拼成数据串
        s.append("merchantId=").append(merchantId);
        s.append("&responseMode=").append(responseMode);
        s.append("&orderId=").append(orderId);
        s.append("&currencyType=").append(currencyType);
        s.append("&amount=").append(amount);
        s.append("&returnCode=").append(returnCode);
        s.append("&returnMessage=").append(returnMessage);
        s.append("&merchantKey=").append(merchantKey);
//md5加密
        String nowMac = MD5.md5(s.toString()).toUpperCase();
        Map<String, Object> result = Maps.newHashMap();
        if (nowMac.equals(mac)) { //若mac校验匹配
            if (success || paid) {
                result.put("status", "1");
                result.put("total_fee", amount);
                result.put("orderno", orderId);
                result.put("channel", "WY");
            } else {
                result.put("status", "-1");
            }

        } else { //若mac校验不匹配
            if (success || paid) {
                success = false;
                paid = false;
                returnCode = "0401";
                returnMessage = "mac值校验错误！";
            }
        }

        return result;
    }

    @Override
    public Map<String, Object> getOrder(String orderNumber) {
        return null;
    }

    public void payCall(HttpServletRequest request){

        String accountId = request.getParameter("accountId");
        String orderId = request.getParameter("orderId");
        String amount = request.getParameter("amount");
        String result_code = request.getParameter("result_code");
        String result_msg = request.getParameter("result_msg");

        StringBuffer s = new StringBuffer();
        s.append("accountId").append(accountId)
        .append("&orderId").append(orderId)
        .append("&amount").append(amount)
        .append("&result_code").append(result_code)
        .append("&result_msg").append(result_msg)
        .append("&key").append(MERCHANT_KEY);
        String mac = MD5.md5(s.toString()).toUpperCase();

        String rmac = request.getParameter("mac");
        if(!mac.equals(rmac))
        {
//            return "fail";

        }




    }

    @Override
    public Map<String, Object> settleOrder(MerchantSettleOrder selectByPrimaryKey) {

        Map<String,String> sendParam = Maps.newHashMap();

        String accountId = MERCHANT_NO;
        String name = selectByPrimaryKey.getAccountName();
        String cardNo = selectByPrimaryKey.getMerchantBindBank();
        String orderId = selectByPrimaryKey.getOrderNumber();
        String purpose = "提现";
        String amount = selectByPrimaryKey.getPostalAmount().setScale(BigDecimal.ROUND_HALF_DOWN,2).toString();
        String responseUrl = "http://localhost:8080";
        String businessType = "140001";
        String version = "1.0.1";

        StringBuilder s = new StringBuilder();
        s.append("accountId=")
                .append(accountId)
                .append("&name=").append(name)
                 .append("&cardNo=").append(cardNo)
                 .append("&orderId=").append(orderId)
                 .append("&purpose=").append(purpose)
                 .append("&amount=").append(amount)
                 .append("&responseUrl=").append(responseUrl)
                 .append("&businessType=").append(businessType)
                 .append("&version=").append(version)
                 .append("&key=").append(MERCHANT_KEY);
        String mac = MD5.md5(s.toString()).toUpperCase();

        sendParam.put("accountId",accountId);
        sendParam.put("name",name);
        sendParam.put("cardNo",cardNo);
        sendParam.put("orderId",orderId);
        sendParam.put("purpose",purpose);
        sendParam.put("amount",amount);
        sendParam.put("responseUrl",responseUrl);
        sendParam.put("businessType",businessType);
        sendParam.put("version",version);
        sendParam.put("mac",mac);

        HttpRequestDetailVo rtn = HttpClientUtil.httpPost("https://unspay.com/elegate-pay-front/delegatePay/pay",null,sendParam);

        Map<String,Object> resultEnd = Maps.newHashMap();
        try {
            Map map = JSON.parseObject(rtn.getResultAsString(),Map.class);

            Object code = map.get("result_code");
            if(null!= code && "0000".equals(code)){
                resultEnd.put("status", "1");
            }else{
                resultEnd.put("status","0");
            }

        } catch (Exception e) {

        }

        return resultEnd;
    }


    @Override
    public Map<String, Object> placeOrder(MerchantPlaceOrder selectByPrimaryKey) {
        Map<String,String> sendParam = Maps.newHashMap();

        String accountId = MERCHANT_NO;
        String name = selectByPrimaryKey.getAccountName();
        String cardNo = selectByPrimaryKey.getBankNumber();
        String orderId = selectByPrimaryKey.getOrderNumber();
        String purpose = "提现";
        String amount = selectByPrimaryKey.getPayAmount().setScale(BigDecimal.ROUND_HALF_DOWN,2).toString();
        String responseUrl = "http://localhost:8080";
        String businessType = "140001";
        String version = "1.0.1";

        StringBuilder s = new StringBuilder();
        s.append("accountId=")
                .append(accountId)
                .append("&name=").append(name)
                .append("&cardNo=").append(cardNo)
                .append("&orderId=").append(orderId)
                .append("&purpose=").append(purpose)
                .append("&amount=").append(amount)
                .append("&responseUrl=").append(responseUrl)
                .append("&businessType=").append(businessType)
                .append("&version=").append(version)
                .append("&key=").append(MERCHANT_KEY);
        String mac = MD5.md5(s.toString()).toUpperCase();

        sendParam.put("accountId",accountId);
        sendParam.put("name",name);
        sendParam.put("cardNo",cardNo);
        sendParam.put("orderId",orderId);
        sendParam.put("purpose",purpose);
        sendParam.put("amount",amount);
        sendParam.put("responseUrl",responseUrl);
        sendParam.put("businessType",businessType);
        sendParam.put("version",version);
        sendParam.put("mac",mac);

        HttpRequestDetailVo rtn = HttpClientUtil.httpPost("https://unspay.com/elegate-pay-front/delegatePay/pay",null,sendParam);

        Map<String,Object> resultEnd = Maps.newHashMap();
        try {
            Map map = JSON.parseObject(rtn.getResultAsString(),Map.class);

            Object code = map.get("result_code");
            if(null!= code && "0000".equals(code)){
                resultEnd.put("status", "1");
            }else{
                resultEnd.put("status","0");
            }

        } catch (Exception e) {

        }

        return resultEnd;
    }

    @Override
    public Map<String, Object> taskPayOrderNumber(String orderNumber, Date date) {




        return null;
    }
}
