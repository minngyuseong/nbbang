package com.nbbang.service;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class InstitutionCatalog {
    private final String assetBaseUrl;
    private static final List<Entry> ENTRIES = List.of(
        b("002","KDB산업은행"), b("003","IBK기업은행"), b("004","KB국민은행"), b("007","Sh수협은행"),
        b("011","NH농협은행"), b("020","우리은행"), b("023","SC제일은행"), b("027","한국씨티은행"),
        b("031","iM뱅크"), b("032","부산은행"), b("034","광주은행"), b("035","제주은행"),
        b("037","전북은행"), b("039","경남은행"), b("045","새마을금고"), b("048","신협"),
        b("050","저축은행"), b("054","HSBC은행"), b("055","도이치은행"), b("060","뱅크오브아메리카"),
        b("062","중국공상은행"), b("063","중국건설은행"), b("064","산림조합"), b("071","우체국"), b("081","하나은행"),
        b("088","신한은행"), b("089","케이뱅크"), b("090","카카오뱅크"), b("092","토스뱅크"),
        s("209","유안타증권"), s("218","KB증권"), s("224","BNK투자증권"), s("225","IBK투자증권"),
        s("227","다올투자증권"), s("238","미래에셋증권"), s("240","삼성증권"), s("243","한국투자증권"),
        s("247","NH투자증권"), s("261","교보증권"), s("262","하이투자증권"), s("263","현대차증권"),
        s("264","키움증권"), s("265","LS증권"), s("266","SK증권"), s("267","대신증권"),
        s("269","한화투자증권"), s("270","하나증권"), s("271","토스증권"), s("278","신한투자증권"),
        s("279","DB금융투자"), s("280","유진투자증권"), s("287","메리츠증권"), s("288","카카오페이증권"),
        s("290","부국증권"), s("291","신영증권"), s("292","케이프투자증권"), s("294","한국포스증권")
    );
    public InstitutionCatalog(@Value("${app.asset-base-url}") String assetBaseUrl) { this.assetBaseUrl = assetBaseUrl.replaceAll("/$", ""); }
    public List<View> list() { return ENTRIES.stream().map(e -> new View(e.code(), e.name(), e.category(), assetBaseUrl + "/financial-institutions/" + logoFile(e.code()))).toList(); }
    public Entry require(String code) { return ENTRIES.stream().filter(e -> e.code().equals(code)).findFirst().orElseThrow(() -> new IllegalArgumentException("지원하지 않는 금융기관입니다.")); }
    private static Entry b(String code,String name){return new Entry(code,name,"BANK");}
    private static Entry s(String code,String name){return new Entry(code,name,"SECURITIES");}
    private static String logoFile(String code) {
        return switch (code) { case "224", "225" -> code + ".png"; case "227" -> code + ".ico"; default -> code + ".svg"; };
    }
    public record Entry(String code,String name,String category){}
    public record View(String code,String name,String category,String logoUrl){}
}
