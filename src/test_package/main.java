package test_package;
import edu.stanford.nlp.coref.CorefCoreAnnotations;
import edu.stanford.nlp.coref.data.CorefChain;
import edu.stanford.nlp.coref.data.Dictionaries;
import edu.stanford.nlp.coref.data.Mention;
import edu.stanford.nlp.ie.util.RelationTriple;
import edu.stanford.nlp.io.IOUtils;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.naturalli.NaturalLogicAnnotations;
import edu.stanford.nlp.naturalli.OpenIE;
import edu.stanford.nlp.naturalli.SentenceFragment;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.CoreEntityMention;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.IntPair;
import edu.stanford.nlp.util.PropertiesUtils;
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

//        //stanford pos tagger 형태소 분석기를 이용한 태깅
//        MaxentTagger tagger = new MaxentTagger("taggers/english-left3words-distsim.tagger");
//        String tagged = tagger.tagString(bodyText);
//
//        //핵심 고유단어 추출작업
//        String[] taggedArr = tagged.split(" ");
//        List<String> nnp = Arrays.stream(taggedArr).filter(word -> word.contains("_NNP")).collect(Collectors.toList());
//        Hashtable<String,Integer> freqOfWordTable = new Hashtable<>();
//        for (String word : nnp) {
//            Integer freq = freqOfWordTable.get(word); // 단어를 꺼낸다. word가 key이고 freq가 value
//            freqOfWordTable.put(word, (freq == null) ? 1: freq +1);
//        }
//        List sortedList = sortByValue(freqOfWordTable);
//        String coreNoun = sortedList.get(0).toString();

//        //트리플 추출 과정
//        String[] sentences = tagged.split("\\._\\.");
//        List<String[]> tripples = new ArrayList<>();
//        for (String sentence : sentences){
//            if (sentence.contains(coreNoun)) {
//                String[] words = sentence.split(" ");
//                String subject = "";
//                String predicate = "";
//                String object = "";
//                for (String word:words) {
//                    if(word.equals(coreNoun)) {
//                        String[] removeTag = word.split("_");
//                        subject = removeTag[0];
//                    }else if(word.contains("_VB") && !subject.isEmpty()) {
//                        String[] removeTag = word.split("_");
//                        predicate = removeTag[0];
//                    }else if(word.contains("_NNP") && !predicate.isEmpty()) {
//                        String[] removeTag = word.split("_");
//                        object = removeTag[0];
//                    }
//                    if(!subject.isEmpty() && !predicate.isEmpty() && !object.isEmpty()){
//                        String[] tripple = {subject,predicate,object};
//                        tripples.add(tripple);
//                    }
//                }
//            }
//        }

//        // Jena로 RDF 추출
//        Model model = ModelFactory.createDefaultModel();
//        for(String[] statement : tripples){
//            Resource s = model.createResource("http://subject/"+statement[0]);
//            Property p = model.createProperty("http://predicate/"+statement[1]);
//            RDFNode o = model.createLiteral(statement[2]);
//
//            if(s.hasProperty(p)){
//                s.addProperty(p,model.createResource().addProperty(p,o));
//            }else {
//                s.addProperty(p,o);
//            }
//        }
//        model.write(System.out);
//        //RDFDataMgr.write(System.out, model, Lang.NTRIPLES); // N-TRIPLES 형태로 출력

        // stanford OpenIE
        Properties props = PropertiesUtils.asProperties(
                "annotators", "tokenize,ssplit,pos,lemma,ner,parse,coref"
        );
        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);

        String text;
        if (args.length > 0) {
            text = IOUtils.slurpFile(args[0]);
        } else {
            text = "Korea (officially the \"Korean Peninsula\") is a region in East Asia. Since 1945 it has been divided into the two parts which soon became the two sovereign states: North Korea (officially the \"Democratic People's Republic of Korea\") and South Korea (officially the \"Republic of Korea\"). Korea consists of the Korean Peninsula, Jeju Island, and several minor islands near the peninsula. It is bordered by China to the northwest and Russia to the northeast. It is separated from Japan to the east by the Korea Strait and the Sea of Japan (East Sea). During the first half of the 1st millennium, Korea was divided between the three competing states of Goguryeo, Baekje, and Silla, together known as the Three Kingdoms of Korea.";
            //text = bodyText;
        }


        Annotation docu = new Annotation(text);
        pipeline.annotate(docu);
        List<String> sentList = new ArrayList<>();
        for (CoreMap sentence : docu.get(CoreAnnotations.SentencesAnnotation.class)) {
            sentList.add(sentence.get(CoreAnnotations.TextAnnotation.class));
        }



        String newText = "";
        Collection<CorefChain> values = docu.get(CorefCoreAnnotations.CorefChainAnnotation.class).values();
        for (CorefChain cc : values) {
            //System.out.println("\t" + cc.getMentionsInTextualOrder());
            List<CorefChain.CorefMention> mentionsInTextualOrder = cc.getMentionsInTextualOrder();
            String coreWord = "";
            for (int i = 0; i < mentionsInTextualOrder.size(); i++){
                if (i == 0){
                    coreWord = mentionsInTextualOrder.get(i).mentionSpan; // 첫번째 명사를 원래 명사로 지정
                }
                String mention = mentionsInTextualOrder.get(i).mentionSpan; // 대명사 가져오기
                int sentNum = mentionsInTextualOrder.get(i).sentNum -1; //문장 번호 가져오기
                String modiSent = sentList.get(sentNum); // 수정될 문장 가져오고
                modiSent = modiSent.replaceAll(mention,coreWord); // mention(대명사를) coreWord(원래단어)로 바꿔주고
                sentList.set(sentNum,modiSent); // 수정된 문자열로 바꿔줌
            }
        }

        //System.out.println(sentList);

        for (String s : sentList) {
            newText += s + " ";
        }
        System.out.println(text);
        System.out.println("--------------------------------------------");
        System.out.println(newText);

        System.out.println("\n \n");


        props = PropertiesUtils.asProperties(
                "annotators", "tokenize,ssplit,pos,lemma,parse,natlog,openie"
        );
        props.setProperty("openie.max_entailments_per_clause","100");
        props.setProperty("openie.triple.strict","false");
        pipeline = new StanfordCoreNLP(props);

        docu = new Annotation(newText);
        pipeline.annotate(docu);
        int sentNo = 0;
        for (CoreMap sentence : docu.get(CoreAnnotations.SentencesAnnotation.class)) {
            System.out.println("Sentence #" + ++sentNo + ": " + sentence.get(CoreAnnotations.TextAnnotation.class));

//          // Print SemanticGraph
//          System.out.println(sentence.get(SemanticGraphCoreAnnotations.EnhancedDependenciesAnnotation.class).toString(SemanticGraph.OutputFormat.LIST));

            // Get the OpenIE triples for the sentence
            Collection<RelationTriple> triples = sentence.get(NaturalLogicAnnotations.RelationTriplesAnnotation.class);

            // Print the triples
            for (RelationTriple triple : triples) {
                System.out.println(triple.confidence + "\t" +
                        "<"+triple.subjectGloss()+">" + "\t" +
                        "<"+triple.relationGloss()+">" + "\t" +
                        "<"+triple.objectGloss()+">");
            }
            System.out.println("\n");
            // Alternately, to only run e.g., the clause splitter:
//            List<SentenceFragment> clauses = new OpenIE(props).clausesInSentence(sentence);
//            for (SentenceFragment clause : clauses) {
//                System.out.println(clause.parseTree.toString(SemanticGraph.OutputFormat.LIST));
//            }
//            System.out.println();
        }

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
