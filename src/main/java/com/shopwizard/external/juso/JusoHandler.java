package com.shopwizard.external.juso;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import java.util.ArrayList;
import java.util.List;

public class JusoHandler extends DefaultHandler {

    private Results results;
    private Juso juso;
    private List<Juso> jusoList;
    private StringBuilder content;

    public Results getResults() {
        return results;
    }

    @Override
    public void startDocument() {
        results = new Results();
        jusoList = new ArrayList<>();
        content = new StringBuilder();
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) {
        content = new StringBuilder();
        if ("juso".equals(qName)) juso = new Juso();
    }

    @Override
    public void endElement(String uri, String localName, String qName) {
        String value = content.toString().trim();
        switch (qName) {
            case "totalCount"    -> results.setTotalCount(parseInt(value));
            case "currentPage"   -> results.setCurrentPage(parseInt(value));
            case "countPerPage"  -> results.setCountPerPage(parseInt(value));
            case "errorCode"     -> results.setErrorCode(value);
            case "errorMessage"  -> results.setErrorMessage(value);
            case "roadAddr"      -> juso.setRoadAddr(value);
            case "roadAddrPart1" -> juso.setRoadAddrPart1(value);
            case "roadAddrPart2" -> juso.setRoadAddrPart2(value);
            case "jibunAddr"     -> juso.setJibunAddr(value);
            case "engAddr"       -> juso.setEngAddr(value);
            case "zipNo"         -> juso.setZipNo(value);
            case "admCd"         -> juso.setAdmCd(value);
            case "rnMgtSn"       -> juso.setRnMgtSn(value);
            case "bdMgtSn"       -> juso.setBdMgtSn(value);
            case "detBdNmList"   -> juso.setDetBdNmList(value);
            case "bdNm"          -> juso.setBdNm(value);
            case "bdKdcd"        -> juso.setBdKdcd(value);
            case "siNm"          -> juso.setSiNm(value);
            case "sggNm"         -> juso.setSggNm(value);
            case "emdNm"         -> juso.setEmdNm(value);
            case "liNm"          -> juso.setLiNm(value);
            case "rn"            -> juso.setRn(value);
            case "udrtYn"        -> juso.setUdrtYn(value);
            case "buldMnnm"      -> juso.setBuldMnnm(parseInt(value));
            case "buldSlno"      -> juso.setBuldSlno(parseInt(value));
            case "mtYn"          -> juso.setMtYn(value);
            case "lnbrMnnm"      -> juso.setLnbrMnnm(parseInt(value));
            case "lnbrSlno"      -> juso.setLnbrSlno(parseInt(value));
            case "emdNo"         -> juso.setEmdNo(value);
            case "juso"          -> { jusoList.add(juso); juso = null; }
            case "results"       -> results.setJuso(jusoList);
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) {
        content.append(ch, start, length);
    }

    private int parseInt(String value) {
        try { return Integer.parseInt(value); } catch (Exception e) { return 0; }
    }
}
