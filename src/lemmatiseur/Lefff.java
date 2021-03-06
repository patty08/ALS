package lemmatiseur;

import java.io.*;
import java.lang.*;
import java.util.*;
import tokemisation.*;
import listelemm.*;
import utils.*;
import java.util.regex.Pattern;

/**
 * @brief Permet d'effectuer la lemmatisation d'un texte.
 */

public class Lefff {
	String separator = ""+(char)9;
	String path;
	ArrayList<String> listExp = new ArrayList<String>();
	Tokemiseur arbreVerbe;
	Tokemiseur arbreNomsP;
	Tokemiseur arbreNoms;
	Tokemiseur arbreAdj;
	ListeLemmWrapper Outils;


	/**
	 * @brief Constructeur pour le lefff
	 * @param path chemin d'accés du texte à traiter
	 */
	public Lefff(String path){
		arbreVerbe = new Tokemiseur();
		arbreNomsP = new Tokemiseur();
		arbreNoms = new Tokemiseur();
		arbreAdj = new Tokemiseur();
		Outils = new ListeLemmWrapper();
		this.path = path;
		readLefff();
		readOutil("./res/outils.txt");
		readExp("./res/exp.txt");
	}

	/**
	* @brief Permet la lecture d'un fichier de lefff
	* @detail Permet de lire un fichier de lefff formaté sous la forme : "abaissé v abaisser" afin de préparer la mise à l'infinitif des verbes.
	**/
	public void readLefff(){
		String[] cLine;
		String current = "";

		try{
			File fichier = new File(path);
			BufferedReader txt = new BufferedReader(new InputStreamReader(new FileInputStream(path), "UTF8"));
			String line;
			while ((line = txt.readLine()) != null){
				cLine = line.split(separator);
				if(cLine[1].equalsIgnoreCase("v") || cLine[1].equalsIgnoreCase("vinf") || cLine[3].contains("K##") || cLine[1].equalsIgnoreCase("vprespart")){
					arbreVerbe.createVthree(cLine[0], cLine[2]);
				}
				else if(cLine[1].equalsIgnoreCase("np")){
					//arbreNomsP.createVthree(cLine[0].toLowerCase(),cLine[2].toLowerCase());
					arbreNomsP.createVthree(cLine[0],cLine[2]);
				}
				else if(cLine[1].equalsIgnoreCase("nc"))
					arbreNoms.createVthree(cLine[0],cLine[2]);
				else if(cLine[1].equalsIgnoreCase("adj"))
					arbreAdj.createVthree(cLine[0],cLine[2]);
			}
		}
		catch(Exception e){e.printStackTrace();}
	}

	/**
	 * @brief lit et stock dans un arbre de tokemisation la liste des mots outils
	 * @param path chemin d'accès du fichier texte de mots outils
	 */
	public void readOutil(String path){
		String[] cLine;
		String separator = "\t";
		String current = "";

		try{
			File fichier = new File(path);
			BufferedReader txt = new BufferedReader(new InputStreamReader(new FileInputStream(path), "UTF8"));
			String line;
			while ((line = txt.readLine()) != null){
				cLine = line.split(separator);
				Outils.add(cLine[0], cLine[1]);
			}
		}
		catch(Exception e){e.printStackTrace();}
	}

	/**
	 * @brief permet de charger une liste d'expression
	 * @param path
	 */
	public void readExp(String path){
		try{
			File fichier = new File(path);
			BufferedReader txt = new BufferedReader(new InputStreamReader(new FileInputStream(path), "UTF8"));
			String line;
			while ((line = txt.readLine()) != null){
				listExp.add(line);
			}
		}
		catch(Exception e){e.printStackTrace();}
	}

	/**
	* @brief Ecrit le nouveau texte dans un fichier
	* @param String txt nouveau texte à écrire
	* @param String titre titre du fichier crée
	**/
	public void writeFile(String txt, String titre){
		try{
			String path = "./res/"+titre+".txt";
			File writing = new File(path);

			if(!writing.exists())
				writing.createNewFile();

			FileWriter fw = new FileWriter(writing.getAbsoluteFile());
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write(txt);
			bw.close();
		}
		catch(IOException e){
			e.printStackTrace();
			System.out.println(e);
		}
	}

	/**
	* @brief permet de traiter le texte
	* @detail traite le texte ce trouvant à l'adresse envoyé en paramètre
	* @see writeFile(String txt, String titre)
	* @param String path chemin d'accès du fichier à traiter
	**/
	public String traiteVerbe(String oldTexte){
		//String result = message.replaceAll("%%NAME", name);
		String newText = "";
		String exp = "";
		String tmpWord = "";
		String[] sdSplit;
		exp = findExp(oldTexte);
		String[] split = exp.split(" ");
		String infinitif;

		for(int i = 0; i < split.length; i++){
			//cas particulier ' avant le verbe
			if(split[i].contains("'")){
				sdSplit = split[i].split("'");
				tmpWord = sdSplit[1];
			}
			//cas particulier - après le verbe
			else if(split[i].contains("-") && arbreVerbe.findTokem(tmpWord)==null){
					sdSplit = split[i].split("-");
					tmpWord = sdSplit[0];
			}
			else
				tmpWord = split[i];
			infinitif = arbreVerbe.findTokem(tmpWord);
			//Vérifie qu'il y ait un infinitif
			if(infinitif != null){
				//Vérifie qu'il n'est pas précédé d'un article indéfini
				if(i==0 || (!Outils.find("ad").getArray().contains(split[i-1]) && !Outils.find("ai").getArray().contains(split[i-1]) && !Outils.find("ac").getArray().contains(split[i-1]))){
					split[i] = split[i].replaceFirst(tmpWord, infinitif);
				}
			}
		}
		for(int i = 0; i < split.length; i++){
			newText += split[i]+" ";
		}
		newText += "\n";
		return newText;
	}

	/**
	 * @param oldTexte
	 * @return Le texte traité
	 * @brief Lemmatise tous les noms propres
	 */
	public String traiteNomsP(String oldTexte){
		String newText = oldTexte;
		String mots[];
		String lemmatise;
		mots=oldTexte.split(" ");
		for(String i : mots){
			if((lemmatise=arbreNomsP.findTokem(i))!=null)
				newText=newText.replaceFirst(i,lemmatise);
		}
		return newText;
	}

	/**
	 * @param oldTexte
	 * @return Le texte traité
	 * @brief Lemmatise tous les noms communs
	 */
	public String traiteNoms(String oldTexte){
		String newText = oldTexte;
		String mots[];
		String lemmatise;
		mots=oldTexte.split(" ");
		for(String i : mots){
			if((lemmatise=arbreNoms.findTokem(i))!=null){
				newText=newText.replaceFirst(i,lemmatise);
			}

		}
		return newText;
	}

	/**
	 * @param oldTexte
	 * @return Le texte traité
	 * @brief Lemmatise tous les adjectifs
	 */
	public String traiteAdj(String oldTexte){
		String newText = oldTexte;
		String mots[];
		String lemmatise;
		mots=oldTexte.split(" ");
		for(String i : mots){
			if((lemmatise=arbreAdj.findTokem(i))!=null){
				newText=newText.replaceFirst(i,lemmatise);
			}

		}
		return newText;
	}

	/**
	 * @param oldTexte
	 * @return Le texte traité
	 * @brief Lemmatise tous les noms et adjectifs
	 */
    public String traiteNetAdj(String oldTexte){
        String newText = oldTexte;
        String mots[];
        String lemmatise;
        mots=oldTexte.split(" ");
        for(String i : mots){
			//System.out.println("mot : "+i);
			if(i.equals("heures"))
				System.out.println(arbreNoms.findTokem(i));
            if((lemmatise=arbreNomsP.findTokem(i,false))!=null || (lemmatise=arbreNoms.findTokem(i))!=null || (lemmatise=arbreAdj.findTokem(i))!=null){
                newText=newText.replaceFirst(i,lemmatise);
			}
        }
        return newText;
    }

	/**
	 * @brief vérifie si il y a des expressions à ne pas traiter pour la lemmatisation des verbes
	 * @param line texte à traiter
	 * @return Texte sans les expressions à exclure
	 */
	public String findExp(String line){
		String retour = line;
		for(int i = 0; i < listExp.size(); i++){
			if(line.contains(listExp.get(i))){
				retour = retour.replaceAll(" "+listExp.get(i)+" ", " ");
			}
		}
		return retour;
	}

    public String gardeAdj(String txt,ArrayList<String> adj){
        String newtxt=txt;
        String test=txt;
        String mots[];
        mots=txt.split(" ");
        for(String i : mots){
            if(arbreAdj.findTokem(i)==null || !adj.contains(i)){
				//System.out.println(i);
                newtxt=newtxt.replaceFirst("^"+i+" | "+i+"$","");
                if(newtxt.equals(test))
                    newtxt=newtxt.replaceFirst(" "+i+" "," ");
            }
            test=newtxt;
        }
        return newtxt;
    }

	/**
	 * @brief permet de traiter un texte
	 * @param p path du texte à traiter
	 * @return Le texte traité
	 */
	public String traiteTexte(String p,ArrayList<String> adj){
		Utils U=new Utils();
		String txt = "";
		String tmp="";
		String path = p;
		File f = new File(p);
		if(!f.exists()){
			return "Echec de l'ouverture";
		}
		System.out.println("ouverture du texte");
		txt = U.openTexte(p);
		//txt=txt.toLowerCase();
		System.out.println("Suppression de la ponctuation");
		txt=U.supprPonctuation(txt);
		System.out.println("Traitement des verbes");
		txt = traiteVerbe(txt);
		System.out.println("Traitement des expressions figées neutres");
		txt = supprExpNeutre(txt);

		txt=txt.replaceAll("([A-Z]|[a-z])*’","");
        txt=txt.replaceAll("([A-Z]|[a-z])*'","");
		System.out.println("Traitement des mots outils");
		txt = supprOutils(txt);
		System.out.println("Traitement des noms et adjectifs");
        txt=traiteNetAdj(txt);
		/*txt = traiteNomsP(txt);
		System.out.println("Traitement des Noms Communs");
		txt = traiteNoms(txt);
		System.out.println("Traitement des Adjectifs");
		txt = traiteAdj(txt);*/
		for(int i=0;i<adj.size();i++)
			adj.set(i,traiteAdj(adj.get(i)));
        //txt=gardeAdj(txt,adj);
		//Ecriture du fichier texte de sortie
		String pathSplit[] = path.split("/");
		String titre = pathSplit[pathSplit.length-1];
		System.out.println("titre : "+titre);
		writeFile(txt, titre);
		return txt;
	}

	/**
	 * @brief enlève toute les expressions neutres du texte
	 * @param txt texte à traiter
	 * @return texte traité
	 */
	public String supprExpNeutre(String txt){
		String newTexte = txt;
		String figeList = "";
		String fLine[];
		try{
			File fige = new File("./res/fige.txt");
			BufferedReader expFige = new BufferedReader(new InputStreamReader(new FileInputStream(fige), "UTF8"));
			while ((figeList = expFige.readLine()) != null){
				fLine = figeList.split(separator);
				if(fLine[1].equals("neutre")){
					newTexte = newTexte.replaceAll(fLine[0], "");
				}
			}
		}
		catch(Exception e){
			e.printStackTrace();
			System.out.println(e);
		}
		newTexte=newTexte.replaceAll(" {2,}", " ");
		return newTexte;
	}

	/**
	 * @param txt
	 * @return Le texte traité
	 * @brief Supprime les mots outils
	 */
	 public String supprOutils(String txt){
		 String newTexte=txt;
		 for(ListeLemm i : Outils.getArray())
			for(String j : i.getArray()){
				newTexte=newTexte.replaceAll("(?i) "+j+" "," ");
				newTexte=newTexte.replaceAll("(?i)"+"(^"+j+" | "+j+"$)","");
			}
		return newTexte;
	}

	/**
	 * @return Le tokemiseur des adjectifs
	 * @brief Renvoie l'arbre des adjectifs
	 */
    public Tokemiseur getAdj(){
        return arbreAdj;
    }

	/**
	 * @return Le tokemiseur des Noms
	 * @brief Renvoie l'arbre des Noms
	 */
    public Tokemiseur getNoms(){
        return arbreNoms;
    }

	/**
	 * @return Le tokemiseur des Noms Propres
	 * @brief Renvoie l'arbre des Noms Propres
	 */
    public Tokemiseur getNomsP(){
        return arbreNomsP;
    }
}
