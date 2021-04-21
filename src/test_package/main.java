package test_package;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;
import org.apache.jena.rdf.model.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class main {
    public static void main(String[] args) throws IOException {
        //Jsoup 파싱
        Document doc = Jsoup.connect("https://en.wikipedia.org/wiki/Korea").get();
        Elements pTags = doc.getElementsByTag("p");
        String bodyText = Jsoup.parse(pTags.toString()).text();

        //stanford pos tagger 형태소 분석기를 이용한 태깅
        MaxentTagger tagger = new MaxentTagger("taggers/english-left3words-distsim.tagger");
        String tagged = tagger.tagString(bodyText);

        //핵심 고유단어 추출작업
        String[] taggedArr = tagged.split(" ");
        List<String> nnp = Arrays.stream(taggedArr).filter(word -> word.contains("_NNP")).collect(Collectors.toList());
        Hashtable<String,Integer> freqOfWordTable = new Hashtable<>();
        for (String word : nnp) {
            Integer freq = freqOfWordTable.get(word); // 단어를 꺼낸다. word가 key이고 freq가 value
            freqOfWordTable.put(word, (freq == null) ? 1: freq +1);
        }
        List sortedList = sortByValue(freqOfWordTable);
        String coreNoun = sortedList.get(0).toString();

        //트리플 추출 과정
        String[] sentences = tagged.split("\\._\\.");
        List<String[]> tripples = new ArrayList<>();
        for (String sentence : sentences){
            if (sentence.contains(coreNoun)) {
                String[] words = sentence.split(" ");
                String subject = "";
                String predicate = "";
                String object = "";
                for (String word:words) {
                    if(word.equals(coreNoun)) {
                        String[] removeTag = word.split("_");
                        subject = removeTag[0];
                    }else if(word.contains("_VB") && !subject.isEmpty()) {
                        String[] removeTag = word.split("_");
                        predicate = removeTag[0];
                    }else if(word.contains("_NNP") && !predicate.isEmpty()) {
                        String[] removeTag = word.split("_");
                        object = removeTag[0];
                    }
                    if(!subject.isEmpty() && !predicate.isEmpty() && !object.isEmpty()){
                        String[] tripple = {subject,predicate,object};
                        tripples.add(tripple);
                    }
                }
            }
        }

        // Jena로 RDF 추출
        Model model = ModelFactory.createDefaultModel();
        for(String[] statement : tripples){
            Resource s = model.createResource("http://subject/"+statement[0]);
            Property p = model.createProperty("http://predicate/"+statement[1]);
            RDFNode o = model.createLiteral(statement[2]);

            if(s.hasProperty(p)){
                s.addProperty(p,model.createResource().addProperty(p,o));
            }else {
                s.addProperty(p,o);
            }
        }
        model.write(System.out);
        //RDFDataMgr.write(System.out, model, Lang.NTRIPLES); // N-TRIPLES 형태로 출력

    }
    //map 정렬 메소드
    public static List sortByValue(final Map map) {
        List<String> list = new ArrayList();
        list.addAll(map.keySet());
        Collections.sort(list,new Comparator() {
            public int compare(Object o1,Object o2) {
                Object v1 = map.get(o1);
                Object v2 = map.get(o2);
                return ((Comparable) v2).compareTo(v1);
            }
        });
        //Collections.reverse(list); // 주석시 오름차순
        return list;
    }
}
