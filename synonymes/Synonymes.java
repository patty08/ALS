/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package synonymes;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import java.util.List;
import tokemisation.Tokemiseur;

/**
 * @author juju
 * @version 1.0
 */
public class Synonymes {
    private final ArrayList<String> S;
    private String mot;
    private String language;
    
    /**     
     * @param lang 
     * @brief Construit un objet Synonymes avec une langue spécifique
     */
    public Synonymes(String lang){
        this.language = lang;
        S=new ArrayList<>();
    }
    
    /**
     * @brief Construit un objet Synonymes avec la langue par défaut (fr_FR)
     */
    public Synonymes(){
        this("fr_FR");
    }
        
    /**     
     * @param mot 
     * @brief Retrouve les synonymes d'un mot grâce à l'API de thesaurus.altervista.org et les stocke dans S.
     */
    public void findSynonymes(String mot){         
        try {
            S.clear();
            setMot(mot);
            URL url;     
            //Le format de sortie
            final String output="xml";
            //La clé de l'API
            final String key="RXSY8VaKMzJdzVXUeaCr";
            //Le préfixe du site
            final String site="http://thesaurus.altervista.org/thesaurus/v1";
            url = new URL(site+"?word="+mot+"&language="+language+"&output="+output+"&key="+key);
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();            
            HttpURLConnection conn = (HttpURLConnection)url.openConnection(); 
            conn.setRequestProperty("Accept-Charset", "UTF-8");
            conn.setRequestProperty("Content-Type", "text/plain; charset=utf-8");
            conn.setRequestMethod("GET");
            Document doc;
            XPath p;
            try (BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream(),"UTF-8"))) {
                StringBuilder res=new StringBuilder();
                String line;
                while ((line = rd.readLine()) != null) {
                    res.append(line);
                }   doc = builder.parse(new ByteArrayInputStream(res.toString().getBytes("UTF-8")));
                p = XPathFactory.newInstance().newXPath();          
            }
            NodeList syn = (NodeList)p.compile("/*/*/synonyms").evaluate(doc,XPathConstants.NODESET);
            String[] s;
            for(int i=0;i<syn.getLength();i++){
                s=syn.item(i).getTextContent().split("\\|");
                //Les œ sont mal encodés et deviennent des ½ donc on les remplace
                for(String j : s){                                          
                    j=j.replace('½', 'œ');                    
                    S.add(j);
                }                          
            }            
        } catch (MalformedURLException ex) {
            Logger.getLogger(Synonymes.class.getName()).log(Level.SEVERE, null, ex);                    
        } catch (IOException | ParserConfigurationException | SAXException | XPathExpressionException ex) {
            Logger.getLogger(Synonymes.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * @param Tokemiseur SerieA
     * @param Tokemiseur SerieB
     * @brief Détermine selon les synonymes trouvés si le mot d'origine appartient à la série A ou série B puis l'écrit dans un fichier de mise à jour serieA_maj ou serieB_maj
     */
    public void classification(Tokemiseur SerieA, Tokemiseur SerieB){
        try {
            int A,B;
            A=B=0;
            for(String i : S){
				//Si on trouve le mot dans serie A
                if(SerieA.findTokem(i)!=-1){
                    A++;
                    System.out.println(i+"->serieA");
                }
                //Si on trouve le mot dans serie B
                else if(SerieB.findTokem(i)!=-1){
                    B++;
                    System.out.println(i+"->serieB");
                }
            }
            System.out.println(mot);
            System.out.println("Serie A : "+A);
            System.out.println("Serie B : "+B);
            File file;
            if(A>B)
                file = new File("res/serieA_maj");
            else if(A<B)
                file = new File("res/serieB_maj");
            else
                return;
            if(!file.exists())
                file.createNewFile();
            FileWriter writer = new FileWriter(file,true);
            writer.write(mot+"\n");
            writer.flush();
            writer.close();                                  
        } catch (IOException ex) {
            Logger.getLogger(Synonymes.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

	/**
	 * @param mot
	 * @param Tokemiseur SerieA
	 * @param Tokemiseur SerieB
	 * @brief Détermine si un mot appartient à la série A ou à la série B
	 */
    public void classifierMot(String mot,Tokemiseur SerieA, Tokemiseur SerieB){
        findSynonymes(mot);
        classification(SerieA,SerieB);
    }
	
	/**
	 * @param List<String> L
	 * @param Tokemiseur A
	 * @param Tokemiseur B
	 * @brief Permet de déterminer à quelles séries appartiennent tous les mots d'une liste
	 */
    public void classifierListe(List<String> L,Tokemiseur A,Tokemiseur B){
        for(String i : L)
            classifierMot(i,A,B);   
    }
    
    /**     
     * @param mot
     * @param lang 
     * @brief La même chose que findSynonymes(String mot) mais avec une option de langue
     */
    public void findSynonymes(String mot,String lang){
        setLang(lang);
        findSynonymes(mot);
    }
    
    /**     
     * @return S
     * @brief Renvoie la liste de Synonymes
     */
    public ArrayList<String> getSynonymes(){
        return S;
    }
    
    /**
     * @param mot
     * @brief Règle le mot d'origine
     */
    private void setMot(String mot){
        this.mot=mot;
    }
    
    /**
     * @return String
     * @brief Retourne le mot d'origine pour lequel les synonymes ont été trouvés
     */
    public String getMot(){
        return mot;
    }
    
    /**     
     * @param lang 
     * @brief Permet de régler la langue
     */
    public void setLang(String lang){
        this.language=lang;
    }
    
    /**     
     * @return 
     * @brief Renvoie la langue actuelle
     */
    public String getLang(){
        return language;
    }
}
