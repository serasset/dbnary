package org.getalp.dbnary.fin;

import org.getalp.dbnary.LangTools;

import java.util.HashMap;

public class SuomiLangToCode extends LangTools {

    static HashMap<String, String> nc = new HashMap<String, String>();
    static HashMap<String, String> cn = new HashMap<String, String>();

    static {
        nc.put("kantoninkiina", "zh-yue");
        nc.put("mandariinikiina", "zh");
        nc.put("afar", "aa");
        nc.put("abhaasi", "ab");
        nc.put("adyge", "ady");
        nc.put("avesta", "ae");
        nc.put("afrikaans", "af");
        nc.put("jokin afro-aasialainen kieli", "afa");
        nc.put("afrihili", "afh");
        nc.put("arguni", "agf");
        nc.put("ainu", "ain");
        nc.put("aari", "aiw");
        nc.put("akan", "ak");
        nc.put("akkadi", "akk");
        nc.put("aleutti", "ale");
        nc.put("algonkin-kielet", "alg");
        nc.put("elsassi", "als");
        nc.put("altai", "alt");
        nc.put("amhara", "am");
        nc.put("aragonia", "an");
        nc.put("muinaisenglanti", "ang");
        nc.put("angika", "anp");
        nc.put("sa'a", "apb");
        nc.put("arabia", "ar");
        nc.put("aramea", "arc");
        nc.put("araukaani", "arn");
        nc.put("arapho", "arp");
        nc.put("assami", "as");
        nc.put("asturia", "ast");
        nc.put("avaari", "av");
        nc.put("aimara", "ay");
        nc.put("azeri", "az");
        nc.put("baškiiri", "ba");
        nc.put("bali", "ban");
        nc.put("basa", "bas");
        nc.put("balttilaiset kielet", "bat");
        nc.put("valkovenäjä", "be");
        nc.put("bedža", "bej");
        nc.put("bemba", "bem");
        nc.put("berberikielet", "ber");
        nc.put("bulgaria", "bg");
        nc.put("bihari", "bh");
        nc.put("bima", "bhp");
        nc.put("bislama", "bi");
        nc.put("mustajalka", "bla");
        nc.put("bambara", "bm");
        nc.put("bengali", "bn");
        nc.put("bantukielet", "bnt");
        nc.put("bintulu", "bny");
        nc.put("tiibet", "bo");
        nc.put("bretoni", "br");
        nc.put("bosnia", "bs");
        nc.put("blin", "byn");
        nc.put("babuza", "bzg");
        nc.put("buli", "bzq");
        nc.put("katalaani", "ca");
        nc.put("caddo", "cad");
        nc.put("keskiamerikkalaiset intiaanikielet", "cai");
        nc.put("karoliini", "cal");
        nc.put("karibi", "car");
        nc.put("tšetšeeni", "ce");
        nc.put("cebuano", "ceb");
        nc.put("tšamorro", "ch");
        nc.put("tšagatai", "chg");
        nc.put("chuuk", "chk");
        nc.put("mari", "chm");
        nc.put("choctaw", "cho");
        nc.put("cherokee", "chr");
        nc.put("cheyenne", "chy");
        nc.put("kavalan", "ckv");
        nc.put("shilluk", "cl");
        nc.put("tsam-kielet", "cmc");
        nc.put("mandariinikiina", "cmn");
        nc.put("korsika", "co");
        nc.put("kopti", "cop");
        nc.put("englantilaiset kreolikielet", "cpe");
        nc.put("ranskalaiset kreolikielet", "cpf");
        nc.put("portugalilaiset kreolikielet", "cpp");
        nc.put("cree", "cr");
        nc.put("krimintataari", "crh");
        nc.put("tšekki", "cs");
        nc.put("kašubi", "csb");
        nc.put("kirkkoslaavi", "cu");
        nc.put("tšuvassi", "cv");
        nc.put("kymri", "cy");
        nc.put("tanska", "da");
        nc.put("saksa", "de");
        nc.put("dogri", "doi");
        nc.put("dravidakielet", "dra");
        nc.put("alasorbi", "dsb");
        nc.put("keskihollanti", "dum");
        nc.put("divehi", "dv");
        nc.put("djula", "dyu");
        nc.put("dzongkha", "dz");
        nc.put("ewe", "ee");
        nc.put("efik", "efi");
        nc.put("kreikka", "el");
        nc.put("englanti", "en");
        nc.put("keskienglanti", "enm");
        nc.put("esperanto", "eo");
        nc.put("espanja", "es");
        nc.put("viro", "et");
        nc.put("baski", "eu");
        nc.put("persia", "fa");
        nc.put("fulani", "ff");
        nc.put("suomi", "fi");
        nc.put("filipino", "fil");
        nc.put("jokin suomalais-ugrilainen kieli", "fiu");
        nc.put("karjala", "fiu-kar");
        nc.put("võro", "fiu-vro");
        nc.put("fidži", "fj");
        nc.put("fääri", "fo");
        nc.put("siraya", "fos");
        nc.put("ranska", "fr");
        nc.put("keskiranska", "frm");
        nc.put("muinaisranska", "fro");
        nc.put("arpitaani", "frp");
        nc.put("friuli", "fur");
        nc.put("friisi", "fy");
        nc.put("iiri", "ga");
        nc.put("gayo", "gay");
        nc.put("gbaja", "gba");
        nc.put("gaeli", "gd");
        nc.put("gedaged", "gdd");
        nc.put("germaaniset kielet", "gem");
        nc.put("kiribati", "gil");
        nc.put("galego", "gl");
        nc.put("keskiyläsaksa", "gmh");
        nc.put("guarani", "gn");
        nc.put("muinaisyläsaksa", "goh");
        nc.put("gondi", "gon");
        nc.put("gorontalo", "gor");
        nc.put("gootti", "got");
        nc.put("muinaiskreikka", "grc");
        nc.put("gari", "gri");
        nc.put("gudžarati", "gu");
        nc.put("manksi", "gv");
        nc.put("gwich'in", "gwi");
        nc.put("hausa", "ha");
        nc.put("havaiji", "haw");
        nc.put("heprea", "he");
        nc.put("hindi", "hi");
        nc.put("hiri motu", "ho");
        nc.put("kroatia", "hr");
        nc.put("yläsorbi", "hsb");
        nc.put("haitinkreoli", "ht");
        nc.put("unkari", "hu");
        nc.put("hupa", "hup");
        nc.put("armenia", "hy");
        nc.put("herero", "hz");
        nc.put("interlingua", "ia");
        nc.put("iban", "iba");
        nc.put("igbo", "ibo");
        nc.put("indonesia", "id");
        nc.put("interlingue", "ie");
        nc.put("igbo", "ig");
        nc.put("nuosu", "ii");
        nc.put("inupiatun", "ik");
        nc.put("iloko", "ilo");
        nc.put("inguuši", "inh");
        nc.put("inkeroinen", "ink");
        nc.put("ido", "io");
        nc.put("iranilaiset kielet", "ira");
        nc.put("islanti", "is");
        nc.put("italia", "it");
        nc.put("inuktitut", "iu");
        nc.put("japani", "ja");
        nc.put("lojban", "jbo");
        nc.put("juutalaispersia", "jpr");
        nc.put("juutalaisarabia", "jrb");
        nc.put("jaava", "jv");
        nc.put("georgia", "ka");
        nc.put("karakalpakki", "kaa");
        nc.put("kabardi", "kbd");
        nc.put("kongo", "kg");
        nc.put("saka", "kho");
        nc.put("kikuju", "ki");
        nc.put("kilivila", "kij");
        nc.put("kwanjama", "kj");
        nc.put("kazakki", "kk");
        nc.put("grönlanti", "kl");
        nc.put("khmer", "km");
        nc.put("kannada", "kn");
        nc.put("korea", "ko");
        nc.put("kosrae", "kos");
        nc.put("koyukon", "koy");
        nc.put("kapingamarangi", "kpg");
        nc.put("kanuri", "kr");
        nc.put("karatšai-balkaari", "krc");
        nc.put("karjala", "krl");
        nc.put("kašmiri", "ks");
        nc.put("kurdi", "ku");
        nc.put("'auhelawa", "kud");
        nc.put("komi", "kv");
        nc.put("kove", "kvc");
        nc.put("korni", "kw");
        nc.put("kwaio", "kwd");
        nc.put("kwara'ae", "kwf");
        nc.put("kairiru", "kxa");
        nc.put("kirgiisi", "ky");
        nc.put("kayupulau", "kzu");
        nc.put("latina", "la");
        nc.put("ladino", "lad");
        nc.put("lamba", "lam");
        nc.put("luxemburg", "lb");
        nc.put("wampar", "lbq");
        nc.put("ganda", "lg");
        nc.put("lahanan", "lhn");
        nc.put("limburgi", "li");
        nc.put("likum", "lib");
        nc.put("liivi", "liv");
        nc.put("lampung", "ljp");
        nc.put("lau", "llu");
        nc.put("hano", "lml");
        nc.put("lingala", "ln");
        nc.put("lao", "lo");
        nc.put("lou", "loj");
        nc.put("lozi", "loz");
        nc.put("liettua", "lt");
        nc.put("latgalli", "ltg");
        nc.put("luba", "lu");
        nc.put("luba (Lulua)", "lua");
        nc.put("latvia", "lv");
        nc.put("madura", "mad");
        nc.put("magahi", "mag");
        nc.put("mokša", "mdf");
        nc.put("motu", "meu");
        nc.put("mbembe", "mfn");
        nc.put("malagassi", "mg");
        nc.put("mailu", "mgu");
        nc.put("marshall", "mh");
        nc.put("buru", "mhs");
        nc.put("maori", "mi");
        nc.put("makedonia", "mk");
        nc.put("mon-khmer-kielet", "mkh");
        nc.put("malajalam", "ml");
        nc.put("mongoli", "mn");
        nc.put("mantšu", "mnc");
        nc.put("manipuri", "mni");
        nc.put("manobo-kielet", "mno");
        nc.put("moldavia", "mo");
        nc.put("manggarai", "mqy");
        nc.put("marathi", "mr");
        nc.put("marquesas", "mrq");
        nc.put("mangareva", "mrv");
        nc.put("malaiji", "ms");
        nc.put("malta", "mt");
        nc.put("mota", "mtt");
        nc.put("creek", "mus");
        nc.put("manam", "mva");
        nc.put("burma", "my");
        nc.put("ersä", "myv");
        nc.put("nauru", "na");
        nc.put("nahuatl", "nah");
        nc.put("napoli", "nap");
        nc.put("norja (bokmål)", "nb");
        nc.put("nauna", "ncn");
        nc.put("pohjois-ndebele", "nd");
        nc.put("alasaksa", "nds");
        nc.put("hollannin alasaksa", "nds-nl");
        nc.put("nepali", "ne");
        nc.put("nengone", "nen");
        nc.put("ndonga", "ng");
        nc.put("nigeriläis-kongolaiset kielet", "nic");
        nc.put("niue", "niu");
        nc.put("nukuoro", "nkr");
        nc.put("hollanti", "nl");
        nc.put("norja (nynorsk)", "nn");
        nc.put("norja (bokmål)", "no");
        nc.put("novial", "nov");
        nc.put("n'ko", "nqo");
        nc.put("etelä-ndebele", "nr");
        nc.put("pohjoissotho", "nso");
        nc.put("navajo", "nv");
        nc.put("ngadha", "nxg");
        nc.put("njandža", "ny");
        nc.put("nzima", "nzi");
        nc.put("oksitaani", "oc");
        nc.put("ojibwe", "oj");
        nc.put("oromo", "om");
        nc.put("orija", "or");
        nc.put("osseetti", "os");
        nc.put("osmani", "ota");
        nc.put("punjabi", "pa");
        nc.put("kapampangan", "pam");
        nc.put("palau", "pau");
        nc.put("aklanon", "phi");
        nc.put("foinikia", "phn");
        nc.put("paali", "pi");
        nc.put("pitjantjatjara", "pjt");
        nc.put("pukapuka", "pkp");
        nc.put("puola", "pl");
        nc.put("pamona", "pmf");
        nc.put("tuamotu", "pmt");
        nc.put("ponape", "pon");
        nc.put("prakit-kielet", "pra");
        nc.put("muinaisoksitaani", "pro");
        nc.put("paštu", "ps");
        nc.put("portugali", "pt");
        nc.put("paiwan", "pwn");
        nc.put("pyuma", "pyu");
        nc.put("ketšua", "qu");
        nc.put("rapanui", "rap");
        nc.put("rarotonga", "rar");
        nc.put("retoromaani", "rm");
        nc.put("suomen romani", "rmf");
        nc.put("vlaxinromani", "rmy");
        nc.put("kirundi", "rn");
        nc.put("romania", "ro");
        nc.put("romani", "rom");
        nc.put("ririo", "rri");
        nc.put("rotuma", "rtm");
        nc.put("venäjä", "ru");
        nc.put("aromania", "rup");
        nc.put("kinjaruanda", "rw");
        nc.put("sanskrit", "sa");
        nc.put("jakuutti", "sah");
        nc.put("sasak", "sas");
        nc.put("santali", "sat");
        nc.put("sardi", "sc");
        nc.put("sisilia", "scn");
        nc.put("skotti", "sco");
        nc.put("sindhi", "sd");
        nc.put("pohjoissaame", "se");
        nc.put("selkuppi", "sel");
        nc.put("seemiläiset kielet", "sem");
        nc.put("sango", "sg");
        nc.put("muinaisiiri", "sga");
        nc.put("viittomakielet", "sgn");
        nc.put("serbokroaatti", "sh");
        nc.put("sinhali", "si");
        nc.put("slovakki", "sk");
        nc.put("sikaiana", "sky");
        nc.put("sloveeni", "sl");
        nc.put("samoa", "sm");
        nc.put("eteläsaame", "sma");
        nc.put("luulajansaame", "smj");
        nc.put("inarinsaame", "smn");
        nc.put("koltansaame", "sms");
        nc.put("shona", "sn");
        nc.put("soninke", "snk");
        nc.put("somali", "so");
        nc.put("sogdi", "sog");
        nc.put("songhai", "son");
        nc.put("albania", "sq");
        nc.put("serbia", "sr");
        nc.put("swazi", "ss");
        nc.put("thao", "ssf");
        nc.put("seimat", "ssg");
        nc.put("sengseng", "ssz");
        nc.put("sotho", "st");
        nc.put("sunda", "su");
        nc.put("susu", "sus");
        nc.put("sumeri", "sux");
        nc.put("ruotsi", "sv");
        nc.put("swahili", "sw");
        nc.put("saaroa", "sxr");
        nc.put("tamili", "ta");
        nc.put("atayal", "tay");
        nc.put("telugu", "te");
        nc.put("temne", "tem");
        nc.put("terêna", "ter");
        nc.put("tetum", "tet");
        nc.put("tadžikki", "tg");
        nc.put("tigak", "tgc");
        nc.put("thai", "th");
        nc.put("tigrinja", "ti");
        nc.put("turkmeeni", "tk");
        nc.put("tokelau", "tkl");
        nc.put("teanu", "tkw");
        nc.put("tagalog", "tl");
        nc.put("klingon", "tlh");
        nc.put("levei", "tlx");
        nc.put("tswana", "tn");
        nc.put("tonga", "to");
        nc.put("toki pona", "tokipona");
        nc.put("tok pisin", "tpi");
        nc.put("turkki", "tr");
        nc.put("taroko", "trv");
        nc.put("tsonga", "ts");
        nc.put("tsimsi", "tsi");
        nc.put("tataari", "tt");
        nc.put("tumbuka", "tum");
        nc.put("tupi-kielet", "tup");
        nc.put("altailaiset kielet", "tut");
        nc.put("tuvalu", "tvl");
        nc.put("twi", "tw");
        nc.put("tahiti", "ty");
        nc.put("tuva", "tyv");
        nc.put("udmurtti", "udm");
        nc.put("uiguuri", "ug");
        nc.put("ukraina", "uk");
        nc.put("urdu", "ur");
        nc.put("uzbekki", "uz");
        nc.put("venda", "ve");
        nc.put("vepsä", "vep");
        nc.put("vietnam", "vi");
        nc.put("volapük", "vo");
        nc.put("vatja", "vot");
        nc.put("võro", "vro");
        nc.put("valloni", "wa");
        nc.put("watubela", "wah");
        nc.put("waray", "war");
        nc.put("sorbi", "wen");
        nc.put("wanukaka", "wnk");
        nc.put("wolof", "wo");
        nc.put("shanghainkiina", "wuu");
        nc.put("wuvulu", "wuv");
        nc.put("kalmukki", "xal");
        nc.put("kambera", "xbr");
        nc.put("xhosa", "xh");
        nc.put("kanakanabu", "xnb");
        nc.put("saisiat", "xsy");
        nc.put("yap", "yap");
        nc.put("jiddiš", "yi");
        nc.put("joruba", "yo");
        nc.put("jupikkikielet", "ypk");
        nc.put("kantoninkiina", "yue");
        nc.put("zhuang", "za");
        nc.put("zenaga", "zen");
        nc.put("min-kiina", "zh-min-nan");
        nc.put("zulu", "zu");


        cn.put("zh-yue", "kantoninkiina");
        cn.put("zh", "mandariinikiina");
        cn.put("aa", "afar");
        cn.put("ab", "abhaasi");
        cn.put("ady", "adyge");
        cn.put("ae", "avesta");
        cn.put("af", "afrikaans");
        cn.put("afa", "jokin afro-aasialainen kieli");
        cn.put("afh", "afrihili");
        cn.put("agf", "arguni");
        cn.put("ain", "ainu");
        cn.put("aiw", "aari");
        cn.put("ak", "akan");
        cn.put("akk", "akkadi");
        cn.put("ale", "aleutti");
        cn.put("alg", "algonkin-kielet");
        cn.put("als", "elsassi");
        cn.put("alt", "altai");
        cn.put("am", "amhara");
        cn.put("an", "aragonia");
        cn.put("ang", "muinaisenglanti");
        cn.put("anp", "angika");
        cn.put("apb", "sa'a");
        cn.put("ar", "arabia");
        cn.put("arc", "aramea");
        cn.put("arn", "araukaani");
        cn.put("arp", "arapho");
        cn.put("as", "assami");
        cn.put("ast", "asturia");
        cn.put("av", "avaari");
        cn.put("ay", "aimara");
        cn.put("az", "azeri");
        cn.put("ba", "baškiiri");
        cn.put("ban", "bali");
        cn.put("bas", "basa");
        cn.put("bat", "balttilaiset kielet");
        cn.put("be", "valkovenäjä");
        cn.put("bej", "bedža");
        cn.put("bem", "bemba");
        cn.put("ber", "berberikielet");
        cn.put("bg", "bulgaria");
        cn.put("bh", "bihari");
        cn.put("bhp", "bima");
        cn.put("bi", "bislama");
        cn.put("bla", "mustajalka");
        cn.put("bm", "bambara");
        cn.put("bn", "bengali");
        cn.put("bnt", "bantukielet");
        cn.put("bny", "bintulu");
        cn.put("bo", "tiibet");
        cn.put("br", "bretoni");
        cn.put("bs", "bosnia");
        cn.put("byn", "blin");
        cn.put("bzg", "babuza");
        cn.put("bzq", "buli");
        cn.put("ca", "katalaani");
        cn.put("cad", "caddo");
        cn.put("cai", "keskiamerikkalaiset intiaanikielet");
        cn.put("cal", "karoliini");
        cn.put("car", "karibi");
        cn.put("ce", "tšetšeeni");
        cn.put("ceb", "cebuano");
        cn.put("ch", "tšamorro");
        cn.put("chg", "tšagatai");
        cn.put("chk", "chuuk");
        cn.put("chm", "mari");
        cn.put("cho", "choctaw");
        cn.put("chr", "cherokee");
        cn.put("chy", "cheyenne");
        cn.put("ckv", "kavalan");
        cn.put("cl", "shilluk");
        cn.put("cmc", "tsam-kielet");
        cn.put("cmn", "mandariinikiina");
        cn.put("co", "korsika");
        cn.put("cop", "kopti");
        cn.put("cpe", "englantilaiset kreolikielet");
        cn.put("cpf", "ranskalaiset kreolikielet");
        cn.put("cpp", "portugalilaiset kreolikielet");
        cn.put("cr", "cree");
        cn.put("crh", "krimintataari");
        cn.put("cs", "tšekki");
        cn.put("csb", "kašubi");
        cn.put("cu", "kirkkoslaavi");
        cn.put("cv", "tšuvassi");
        cn.put("cy", "kymri");
        cn.put("da", "tanska");
        cn.put("de", "saksa");
        cn.put("doi", "dogri");
        cn.put("dra", "dravidakielet");
        cn.put("dsb", "alasorbi");
        cn.put("dum", "keskihollanti");
        cn.put("dv", "divehi");
        cn.put("dyu", "djula");
        cn.put("dz", "dzongkha");
        cn.put("ee", "ewe");
        cn.put("efi", "efik");
        cn.put("el", "kreikka");
        cn.put("en", "englanti");
        cn.put("enm", "keskienglanti");
        cn.put("eo", "esperanto");
        cn.put("es", "espanja");
        cn.put("et", "viro");
        cn.put("eu", "baski");
        cn.put("fa", "persia");
        cn.put("ff", "fulani");
        cn.put("fi", "suomi");
        cn.put("fil", "filipino");
        cn.put("fiu", "jokin suomalais-ugrilainen kieli");
        cn.put("fiu-kar", "karjala");
        cn.put("fiu-vro", "võro");
        cn.put("fj", "fidži");
        cn.put("fo", "fääri");
        cn.put("fos", "siraya");
        cn.put("fr", "ranska");
        cn.put("frm", "keskiranska");
        cn.put("fro", "muinaisranska");
        cn.put("frp", "arpitaani");
        cn.put("fur", "friuli");
        cn.put("fy", "friisi");
        cn.put("ga", "iiri");
        cn.put("gay", "gayo");
        cn.put("gba", "gbaja");
        cn.put("gd", "gaeli");
        cn.put("gdd", "gedaged");
        cn.put("gem", "germaaniset kielet");
        cn.put("gil", "kiribati");
        cn.put("gl", "galego");
        cn.put("gmh", "keskiyläsaksa");
        cn.put("gn", "guarani");
        cn.put("goh", "muinaisyläsaksa");
        cn.put("gon", "gondi");
        cn.put("gor", "gorontalo");
        cn.put("got", "gootti");
        cn.put("grc", "muinaiskreikka");
        cn.put("gri", "gari");
        cn.put("gu", "gudžarati");
        cn.put("gv", "manksi");
        cn.put("gwi", "gwich'in");
        cn.put("ha", "hausa");
        cn.put("haw", "havaiji");
        cn.put("he", "heprea");
        cn.put("hi", "hindi");
        cn.put("ho", "hiri motu");
        cn.put("hr", "kroatia");
        cn.put("hsb", "yläsorbi");
        cn.put("ht", "haitinkreoli");
        cn.put("hu", "unkari");
        cn.put("hup", "hupa");
        cn.put("hy", "armenia");
        cn.put("hz", "herero");
        cn.put("ia", "interlingua");
        cn.put("iba", "iban");
        cn.put("ibo", "igbo");
        cn.put("id", "indonesia");
        cn.put("ie", "interlingue");
        cn.put("ig", "igbo");
        cn.put("ii", "nuosu");
        cn.put("ik", "inupiatun");
        cn.put("ilo", "iloko");
        cn.put("inh", "inguuši");
        cn.put("ink", "inkeroinen");
        cn.put("io", "ido");
        cn.put("ira", "iranilaiset kielet");
        cn.put("is", "islanti");
        cn.put("it", "italia");
        cn.put("iu", "inuktitut");
        cn.put("ja", "japani");
        cn.put("jbo", "lojban");
        cn.put("jpr", "juutalaispersia");
        cn.put("jrb", "juutalaisarabia");
        cn.put("jv", "jaava");
        cn.put("ka", "georgia");
        cn.put("kaa", "karakalpakki");
        cn.put("kbd", "kabardi");
        cn.put("kg", "kongo");
        cn.put("kho", "saka");
        cn.put("ki", "kikuju");
        cn.put("kij", "kilivila");
        cn.put("kj", "kwanjama");
        cn.put("kk", "kazakki");
        cn.put("kl", "grönlanti");
        cn.put("km", "khmer");
        cn.put("kn", "kannada");
        cn.put("ko", "korea");
        cn.put("kos", "kosrae");
        cn.put("koy", "koyukon");
        cn.put("kpg", "kapingamarangi");
        cn.put("kr", "kanuri");
        cn.put("krc", "karatšai-balkaari");
        cn.put("krl", "karjala");
        cn.put("ks", "kašmiri");
        cn.put("ku", "kurdi");
        cn.put("kud", "'auhelawa");
        cn.put("kv", "komi");
        cn.put("kvc", "kove");
        cn.put("kw", "korni");
        cn.put("kwd", "kwaio");
        cn.put("kwf", "kwara'ae");
        cn.put("kxa", "kairiru");
        cn.put("ky", "kirgiisi");
        cn.put("kzu", "kayupulau");
        cn.put("la", "latina");
        cn.put("lad", "ladino");
        cn.put("lam", "lamba");
        cn.put("lb", "luxemburg");
        cn.put("lbq", "wampar");
        cn.put("lg", "ganda");
        cn.put("lhn", "lahanan");
        cn.put("li", "limburgi");
        cn.put("lib", "likum");
        cn.put("liv", "liivi");
        cn.put("ljp", "lampung");
        cn.put("llu", "lau");
        cn.put("lml", "hano");
        cn.put("ln", "lingala");
        cn.put("lo", "lao");
        cn.put("loj", "lou");
        cn.put("loz", "lozi");
        cn.put("lt", "liettua");
        cn.put("ltg", "latgalli");
        cn.put("lu", "luba");
        cn.put("lua", "luba (Lulua)");
        cn.put("lv", "latvia");
        cn.put("mad", "madura");
        cn.put("mag", "magahi");
        cn.put("mdf", "mokša");
        cn.put("meu", "motu");
        cn.put("mfn", "mbembe");
        cn.put("mg", "malagassi");
        cn.put("mgu", "mailu");
        cn.put("mh", "marshall");
        cn.put("mhs", "buru");
        cn.put("mi", "maori");
        cn.put("mk", "makedonia");
        cn.put("mkh", "mon-khmer-kielet");
        cn.put("ml", "malajalam");
        cn.put("mn", "mongoli");
        cn.put("mnc", "mantšu");
        cn.put("mni", "manipuri");
        cn.put("mno", "manobo-kielet");
        cn.put("mo", "moldavia");
        cn.put("mqy", "manggarai");
        cn.put("mr", "marathi");
        cn.put("mrq", "marquesas");
        cn.put("mrv", "mangareva");
        cn.put("ms", "malaiji");
        cn.put("mt", "malta");
        cn.put("mtt", "mota");
        cn.put("mus", "creek");
        cn.put("mva", "manam");
        cn.put("my", "burma");
        cn.put("myv", "ersä");
        cn.put("na", "nauru");
        cn.put("nah", "nahuatl");
        cn.put("nap", "napoli");
        cn.put("nb", "norja (bokmål)");
        cn.put("ncn", "nauna");
        cn.put("nd", "pohjois-ndebele");
        cn.put("nds", "alasaksa");
        cn.put("nds-nl", "hollannin alasaksa");
        cn.put("ne", "nepali");
        cn.put("nen", "nengone");
        cn.put("ng", "ndonga");
        cn.put("nic", "nigeriläis-kongolaiset kielet");
        cn.put("niu", "niue");
        cn.put("nkr", "nukuoro");
        cn.put("nl", "hollanti");
        cn.put("nn", "norja (nynorsk)");
        cn.put("no", "norja (bokmål)");
        cn.put("nov", "novial");
        cn.put("nqo", "n'ko");
        cn.put("nr", "etelä-ndebele");
        cn.put("nso", "pohjoissotho");
        cn.put("nv", "navajo");
        cn.put("nxg", "ngadha");
        cn.put("ny", "njandža");
        cn.put("nzi", "nzima");
        cn.put("oc", "oksitaani");
        cn.put("oj", "ojibwe");
        cn.put("om", "oromo");
        cn.put("or", "orija");
        cn.put("os", "osseetti");
        cn.put("ota", "osmani");
        cn.put("pa", "punjabi");
        cn.put("pam", "kapampangan");
        cn.put("pau", "palau");
        cn.put("phi", "aklanon");
        cn.put("phn", "foinikia");
        cn.put("pi", "paali");
        cn.put("pjt", "pitjantjatjara");
        cn.put("pkp", "pukapuka");
        cn.put("pl", "puola");
        cn.put("pmf", "pamona");
        cn.put("pmt", "tuamotu");
        cn.put("pon", "ponape");
        cn.put("pra", "prakit-kielet");
        cn.put("pro", "muinaisoksitaani");
        cn.put("ps", "paštu");
        cn.put("pt", "portugali");
        cn.put("pwn", "paiwan");
        cn.put("pyu", "pyuma");
        cn.put("qu", "ketšua");
        cn.put("rap", "rapanui");
        cn.put("rar", "rarotonga");
        cn.put("rm", "retoromaani");
        cn.put("rmf", "suomen romani");
        cn.put("rmy", "vlaxinromani");
        cn.put("rn", "kirundi");
        cn.put("ro", "romania");
        cn.put("rom", "romani");
        cn.put("rri", "ririo");
        cn.put("rtm", "rotuma");
        cn.put("ru", "venäjä");
        cn.put("rup", "aromania");
        cn.put("rw", "kinjaruanda");
        cn.put("sa", "sanskrit");
        cn.put("sah", "jakuutti");
        cn.put("sas", "sasak");
        cn.put("sat", "santali");
        cn.put("sc", "sardi");
        cn.put("scn", "sisilia");
        cn.put("sco", "skotti");
        cn.put("sd", "sindhi");
        cn.put("se", "pohjoissaame");
        cn.put("sel", "selkuppi");
        cn.put("sem", "seemiläiset kielet");
        cn.put("sg", "sango");
        cn.put("sga", "muinaisiiri");
        cn.put("sgn", "viittomakielet");
        cn.put("sh", "serbokroaatti");
        cn.put("si", "sinhali");
        cn.put("sk", "slovakki");
        cn.put("sky", "sikaiana");
        cn.put("sl", "sloveeni");
        cn.put("sm", "samoa");
        cn.put("sma", "eteläsaame");
        cn.put("smj", "luulajansaame");
        cn.put("smn", "inarinsaame");
        cn.put("sms", "koltansaame");
        cn.put("sn", "shona");
        cn.put("snk", "soninke");
        cn.put("so", "somali");
        cn.put("sog", "sogdi");
        cn.put("son", "songhai");
        cn.put("sq", "albania");
        cn.put("sr", "serbia");
        cn.put("ss", "swazi");
        cn.put("ssf", "thao");
        cn.put("ssg", "seimat");
        cn.put("ssz", "sengseng");
        cn.put("st", "sotho");
        cn.put("su", "sunda");
        cn.put("sus", "susu");
        cn.put("sux", "sumeri");
        cn.put("sv", "ruotsi");
        cn.put("sw", "swahili");
        cn.put("sxr", "saaroa");
        cn.put("ta", "tamili");
        cn.put("tay", "atayal");
        cn.put("te", "telugu");
        cn.put("tem", "temne");
        cn.put("ter", "terêna");
        cn.put("tet", "tetum");
        cn.put("tg", "tadžikki");
        cn.put("tgc", "tigak");
        cn.put("th", "thai");
        cn.put("ti", "tigrinja");
        cn.put("tk", "turkmeeni");
        cn.put("tkl", "tokelau");
        cn.put("tkw", "teanu");
        cn.put("tl", "tagalog");
        cn.put("tlh", "klingon");
        cn.put("tlx", "levei");
        cn.put("tn", "tswana");
        cn.put("to", "tonga");
        cn.put("tokipona", "toki pona");
        cn.put("tpi", "tok pisin");
        cn.put("tr", "turkki");
        cn.put("trv", "taroko");
        cn.put("ts", "tsonga");
        cn.put("tsi", "tsimsi");
        cn.put("tt", "tataari");
        cn.put("tum", "tumbuka");
        cn.put("tup", "tupi-kielet");
        cn.put("tut", "altailaiset kielet");
        cn.put("tvl", "tuvalu");
        cn.put("tw", "twi");
        cn.put("ty", "tahiti");
        cn.put("tyv", "tuva");
        cn.put("udm", "udmurtti");
        cn.put("ug", "uiguuri");
        cn.put("uk", "ukraina");
        cn.put("ur", "urdu");
        cn.put("uz", "uzbekki");
        cn.put("ve", "venda");
        cn.put("vep", "vepsä");
        cn.put("vi", "vietnam");
        cn.put("vo", "volapük");
        cn.put("vot", "vatja");
        cn.put("vro", "võro");
        cn.put("wa", "valloni");
        cn.put("wah", "watubela");
        cn.put("war", "waray");
        cn.put("wen", "sorbi");
        cn.put("wnk", "wanukaka");
        cn.put("wo", "wolof");
        cn.put("wuu", "shanghainkiina");
        cn.put("wuv", "wuvulu");
        cn.put("xal", "kalmukki");
        cn.put("xbr", "kambera");
        cn.put("xh", "xhosa");
        cn.put("xnb", "kanakanabu");
        cn.put("xsy", "saisiat");
        cn.put("yap", "yap");
        cn.put("yi", "jiddiš");
        cn.put("yo", "joruba");
        cn.put("ypk", "jupikkikielet");
        cn.put("yue", "kantoninkiina");
        cn.put("za", "zhuang");
        cn.put("zen", "zenaga");
        cn.put("zh-min-nan", "min-kiina");
        cn.put("zu", "zulu");

    }

    public static String threeLettersCode(String s) {
        return threeLettersCode(nc, s);
    }

    public static String getCanonicalCode(String c) {
        String fiLangname = cn.get(c);
        return nc.get(fiLangname);
    }

    public static void main(String[] args) {
        for (String c : cn.keySet()) {
            String fiLangname = cn.get(c);
            String canonicalCode = nc.get(fiLangname);
            String isoCode = LangTools.getCode(canonicalCode);
            String enLangName = LangTools.inEnglish(canonicalCode);

            System.out.format("%s\t%s\t%s\t%s\t%s\n", c, canonicalCode, fiLangname, enLangName, isoCode);
        }
    }
}
