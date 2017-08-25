/**
 *
 */
package org.getalp.dbnary.ita;

import org.getalp.LangTools;

import java.util.HashMap;

/**
 * @author Mariam
 */
public class ItalianLangToCode extends LangTools {

    static HashMap<String, String> h = new HashMap<String, String>();

    static {
        h.put("abcaso", "ab");
        h.put("accado", "akk");
        h.put("accinese", "ace");
        h.put("acioli", "ach");
        h.put("adangme", "ada");
        h.put("afar", "aa");
        h.put("afrihili", "afh");
        h.put("afrikaans", "af");
        h.put("albanese", "sq");
        h.put("alemanno", "gsw");
        h.put("aleuto", "ale");
        h.put("alto tedesco antico (ca. 750-1050)", "goh");
        h.put("alto tedesco medio", "gmh");
        h.put("amarico", "am");
        h.put("antico inglese", "ang");
        h.put("antico slavo ecclesiastico", "cu");
        h.put("arabo", "ar");
        h.put("aragonese", "an");
        h.put("aramaico", "arc");
        h.put("arapaho", "arp");
        h.put("armeno", "hy");
        h.put("aruaco", "arw");
        h.put("assamese", "as");
        h.put("asturiano", "ast");
        h.put("awadhi", "awa");
        h.put("aymara", "ay");
        h.put("azero", "az");
        h.put("balinese", "ban");
        h.put("bambara", "bm");
        h.put("basa", "bas");
        h.put("basco", "eu");
        h.put("bashkir", "ba");
        h.put("basso sassone", "nds");
        h.put("begia", "bej");
        h.put("beluci", "bal");
        h.put("bemba", "bem");
        h.put("bengalese", "bn");
        h.put("bhojpuri", "bho");
        h.put("bhutanese", "dz");
        h.put("bielorusso", "be");
        h.put("bikol", "bik");
        h.put("bini", "bin");
        h.put("birmano", "my");
        h.put("bislama", "bi");
        h.put("blackfoot", "bla");
        h.put("bosniaco", "bs");
        h.put("braj", "bra");
        h.put("bretone", "br");
        h.put("bugi", "bug");
        h.put("bulgaro", "bg");
        h.put("buriat", "bua");
        h.put("caddo", "cad");
        h.put("cambogiano", "km");
        h.put("canarese", "kn");
        h.put("caraibico", "car");
        h.put("casciubico", "csb");
        h.put("catalano", "ca");
        h.put("cebuano", "ceb");
        h.put("ceceno", "ce");
        h.put("ceco", "cs");
        h.put("chamorro", "ch");
        h.put("cherokee", "chr");
        h.put("cheyenne", "chy");
        h.put("chibcha", "chb");
        h.put("chinook", "chn");
        h.put("choctaw", "cho");
        h.put("chuukese", "chk");
        h.put("ciagataico", "chg");
        h.put("cinese", "zh");
        h.put("ciuvascio", "cv");
        h.put("copto", "cop");
        h.put("coreano", "ko");
        h.put("cornico", "kw");
        h.put("corso", "co");
        h.put("Crea le istruzioni!", "man");
        h.put("cree", "cr");
        h.put("croato", "hr");
        h.put("curdo", "ku");
        h.put("dakota", "dak");
        h.put("danese", "da");
        h.put("delaware", "del");
        h.put("dinca", "din");
        h.put("diula", "dyu");
        h.put("dogri", "doi");
        h.put("duala", "dua");
        h.put("ebraico", "he");
        h.put("efik", "efi");
        h.put("egiziano", "egy");
        h.put("ekajuk", "eka");
        h.put("elamitico", "elx");
        h.put("ersiano", "myv");
        h.put("esperanto", "eo");
        h.put("estone", "et");
        h.put("ewe", "ee");
        h.put("ewondo", "ewo");
        h.put("fan", "fan");
        h.put("fanti", "fat");
        h.put("faroese", "fo");
        h.put("fijiano", "fj");
        h.put("filippino", "fil");
        h.put("finlandese", "fi");
        h.put("francese", "fr");
        h.put("francese antico", "fro");
        h.put("francese medio", "frm");
        h.put("frisone", "fy");
        h.put("friulano", "fur");
        h.put("ga", "gaa");
        h.put("ga", "gay");
        h.put("gaelico scozzese", "gd");
        h.put("galiziano", "gl");
        h.put("gallese", "cy");
        h.put("geez", "gez");
        h.put("georgiano", "ka");
        h.put("giapponese", "ja");
        h.put("giavanese", "jv");
        h.put("gilbertino", "gil");
        h.put("giudeo-arabo", "jrb");
        h.put("giudeo-persiano", "jpr");
        h.put("gondi", "gon");
        h.put("gorontalo", "gor");
        h.put("gotico", "got");
        h.put("grebo", "grb");
        h.put("greco", "el");
        h.put("greco antico", "grc");
        h.put("groenlandese", "kl");
        h.put("guaranì", "gn");
        h.put("gujarati", "gu");
        h.put("haida", "hai");
        h.put("haitiano", "ht");
        h.put("hausa", "ha");
        h.put("hawaiano", "haw");
        h.put("hiligayna", "hil");
        h.put("hindi", "hi");
        h.put("hupa", "hup");
        h.put("iban", "iba");
        h.put("ido", "io");
        h.put("ilocano", "ilo");
        h.put("indonesiano", "id");
        h.put("inglese", "en");
        h.put("interlingua", "ia");
        h.put("interlingue", "ie");
        h.put("inuktitut", "iu");
        h.put("inupiaq", "ik");
        h.put("irlandese", "ga");
        h.put("islandese", "is");
        h.put("italiano", "it");
        h.put("kabyle", "kab");
        h.put("kachin", "kac");
        h.put("kalmyk", "xal");
        h.put("kamba", "kam");
        h.put("Kanuri", "kr");
        h.put("karakalpak", "kaa");
        h.put("kashmiri", "ks");
        h.put("kawi", "kaw");
        h.put("kazako", "kk");
        h.put("khasi", "kha");
        h.put("khotanese", "kho");
        h.put("kirghiso", "ky");
        h.put("kirundi", "rn");
        h.put("klingon", "tlh");
        h.put("komi", "kv");
        h.put("konkani", "kok");
        h.put("kosraean", "kos");
        h.put("kpelle", "kpe");
        h.put("kumyk", "kum");
        h.put("kurukh", "kru");
        h.put("kutenai", "kut");
        h.put("lahnda", "lah");
        h.put("lamba", "lam");
        h.put("laotiano", "lo");
        h.put("lappone di Inari", "smn");
        h.put("lappone settentrionale", "se");
        h.put("latino", "la");
        h.put("lettone", "lv");
        h.put("limburghese", "li");
        h.put("lingala", "ln");
        h.put("lituano", "lt");
        h.put("Lojban", "jbo");
        h.put("lusaziano inferiore", "dsb");
        h.put("lusaziano superiore", "hsb");
        h.put("lussemburghese", "lb");
        h.put("macedone", "mk");
        h.put("madurese", "mad");
        h.put("malayalam", "ml");
        h.put("maldiviano", "dv");
        h.put("malese", "ms");
        h.put("malgascio", "mg");
        h.put("maltese", "mt");
        h.put("mancese", "mnc");
        h.put("mannese", "gv");
        h.put("maori", "mi");
        h.put("mapudungun", "arn");
        h.put("maratto", "mr");
        h.put("mari", "chm");
        h.put("marshallese", "mh");
        h.put("masai", "mas");
        h.put("medio inglese", "enm");
        h.put("menangkabau", "min");
        h.put("mirandese", "mwl");
        h.put("mohawk", "moh");
        h.put("moksha", "mdf");
        h.put("moldavo", "mo");
        h.put("mongolo", "mn");
        h.put("napoletano", "nap");
        h.put("naurano", "na");
        h.put("navajo", "ny");
        h.put("ndebele (nord)", "nd");
        h.put("nepalese", "ne");
        h.put("norvegese (bokmål)", "nb");
        h.put("norvegese (bokmål)", "no");
        h.put("norvegese (nynorsk)", "nn");
        h.put("occitanico", "oc");
        h.put("ojibwa", "oj");
        h.put("olandese", "nl");
        h.put("olandese medio", "dum");
        h.put("oriya", "or");
        h.put("oromo", "om");
        h.put("oseto", "os");
        h.put("palauano", "pau");
        h.put("papiamento", "pap");
        h.put("pashto", "ps");
        h.put("persiano", "fa");
        h.put("polacco", "pl");
        h.put("portoghese", "pt");
        h.put("punjabi", "pa");
        h.put("quechua", "qu");
        h.put("rajasthani", "raj");
        h.put("rapanui", "rap");
        h.put("rarotongan", "rar");
        h.put("romancio", "rm");
        h.put("romano", "rom");
        h.put("romeno", "ro");
        h.put("ruandese", "rw");
        h.put("russo", "ru");
        h.put("sacha", "sah");
        h.put("samoano", "sm");
        h.put("sanscrito", "sa");
        h.put("sardo", "sc");
        h.put("scozzese", "sco");
        h.put("serbo", "sr");
        h.put("siciliano", "scn");
        h.put("sindhi", "sd");
        h.put("singalese", "si");
        h.put("slovacco", "sk");
        h.put("sloveno", "sl");
        h.put("somalo", "so");
        h.put("spagnolo", "es");
        h.put("sr per il serbo", "sh");
        h.put("sranan", "srn");
        h.put("sudanese", "su");
        h.put("svedese", "sv");
        h.put("swahili", "sw");
        h.put("swazi", "ss");
        h.put("tagalog", "tl");
        h.put("tagico", "tg");
        h.put("tamil", "ta");
        h.put("tataro", "tt");
        h.put("tataro di Crimea", "crh");
        h.put("tedesco", "de");
        h.put("telugu", "te");
        h.put("tetun", "tet");
        h.put("thailandese", "th");
        h.put("tibetano", "bo");
        h.put("tigrino", "ti");
        h.put("tok pisin", "tpi");
        h.put("tsonga", "ts");
        h.put("tswana", "tn");
        h.put("tumbuka", "tum");
        h.put("turco", "tr");
        h.put("turkmeno", "tk");
        h.put("tuvaluano", "tvl");
        h.put("twi", "tw");
        h.put("ucraino", "uk");
        h.put("udmurt", "udm");
        h.put("uigurico", "ug");
        h.put("ungherese", "hu");
        h.put("urdu", "ur");
        h.put("usbeco", "uz");
        h.put("vallone", "wa");
        h.put("venda", "ve");
        h.put("vietnamita", "vi");
        h.put("volapük", "vo");
        h.put("voto", "vot");
        h.put("waray-waray", "war");
        h.put("wolof", "wo");
        h.put("xhosa", "xh");
        h.put("yapese", "yap");
        h.put("yi del Sichuan", "ii");
        h.put("yiddish", "yi");
        h.put("yoruba", "yo");
        h.put("zhuang", "za");
        h.put("zulu", "zu");
    }

    public static String threeLettersCode(String s) {
        return threeLettersCode(h, s);
    }
}
