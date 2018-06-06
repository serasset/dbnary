parser grammar ATEFgrammaire;

/**
/*! ATEFgrammaire.g4:  compilateur d'ATEF-Y, partie GRAM (grammaire)
 \file
 
 \author JCD
 \date 18/05/2014 (?)
 
\brief compilateur d'ATEF-Y: partie GRAM (grammaire)

ATTENTION: jusqu'à début 2016, ce compilateur était écrit en ANTLR-3, avec target C++.
Suite à un bogue "imbittable", apparu quand JC a voulu faire un exemple complet de 
compilation d'une règle, il a fallu passer en ANTLR-4, donc en target java.

***************************************************************************************
*		=== Commentaires sur la version ANTLR-3 (avant février 2016) ===                *
***************************************************************************************

Remplacement "parsar grammer" -> "grammar" (importation en antlr4).
Avec la gestion d'une pile 'fg fd fg ...' pour la construction des arbres binaires
intervenant dans les affectations et les expressions, les arguments de la 
commande de cre'ation d'un noeud sont re'duits au strict ne'cessaire: type du noeud  
valeur de chai^ne 

Modifications: 

	09/10/2013 (JC): 		pour lever l'ambiguïté introduite par les éventuelles parenthèses 
								ouvrantes devant une relation ("parenthèse d'expression booléenne" 
								u "parenthèse d'expression ensembliste") Jacques Chauché a 
								interdit dans sa grammaire que le premier membre d'une relation 
								commence par une parenthèse ouvrante; elle ne peut apparaître 
								qu'apre`s (cad après le premier terme et le premier opérateur 
								comme dans "CAT(S) -U- (...". Pierre Guillaume n'avait pas lui 
								pris en compte ce point dans sa grammaire.
	
	02/08/2016: ajout dans la règle fonc de la fonction
	17/10/16: suppression des mots-clés DICT, UL, DICT0, UL0 cf Lex.g4 
	17/05/17 (JCD): MODINC n'étant plus considéré comme un mot-clé, on supprime la règle
	litteralOuMODINC et ses appels remplacés par simplement 'Litteral'.

	14/12/2017 (JCD): mise de l'initialisation des structures de données dans la première règle au lieu de la procédure icommande.
			 
// ------------------------------------------------------------------------------------
// Documents:
// 	- Le système ATEF, G-2600-A, octobre 72, J.Chauche', P.Guillaume, M.Quezel-Ambunaz
// 	- Le point sur Ariane-78 de'but 1982, DSE-1, Volume 1, partie C, Le langage ATEF
// 
// On devait avant cette modification changer la grammaire et envisager 2 types de 
// parenthèses: "(" et ")" d'une part et "(-" et "-)" d'autre part, en veillant à
// garder le second type aux relations et non pas aux expressions comme je l'avais fait
// (à cause du cas "(-N- ...").
// 
// Donc on a dans la règle c, l'alternative "PG exbool_ATEF[end] PD" et dans la 
// règle relation_ATEF:
// 
// 	op[end, typevar]
// 	(
// 		(UNION | INTER)		
// 		d1=d[end, typevar] 
// 	)?
// 	rel=relateur_ATEF 
// 	d2=d[end, typevar] 
// 
//  au lieu de:
// 
// 	d1=d[end, typevar] 
// 	rel=relateur_ATEF 
// 	d2=d[end, typevar] 
// ------------------------------------------------------------------------------------
//		10/06/2016: dans la règle op, { attr.det == T_determine.non }?  // !!! n'est pas compilé en antlr4.
//		12/06/2016: suppression dans la règle op des prédicats (sémantique différente d'antlr3) devenus inutiles.
//

// Ci-dessous à supprimer puisqu'il s'agit d'une grammaire importée
// cf warning:
// warning(109): ATEFgrammaire.g:63:0: options ignored in imported grammar ATEFgrammaire


options
{
//c:	language=C;

	// à rajouter si on n'utilise pas la grammaire englobante AY.g
	tokenVocab=Lex;

//c:	output=AST;
//c:	ASTLabelType=pANTLR3_BASE_TREE;

	// backtrack=true; // ne résout rien ligne 173
}
*/

//import Expr, Format;
// ------------------------------------------------------------------------------------

// Dans la liste des règles d'une grammaire on doit avoir une et une seule de chacune
// des 2 règles RDICT et MOTINC. On testera leur existence et leur unicité lors des
// vérifications sémantiques.
regleATEF:
	(
		rdict
	|
		motinc
	|
		idr=Litteral
		DXPTS
			pg_rgl

		EG EG 
			pd_rgl
		POINT		
	);

rdict:
	RDICT
	DXPTS
	PG
		id=Litteral
//		suiteRdict        --- MODIFS 30/11/15 ---
		suiteDicInit
		SLASH
		controle_DU					// contrôle DICT et UL
		(	SLASH
			sens_NI					// sens d'analyse Initial et Mot_inconnu
			(	SLASH
				id2=Litteral		// liste de priorité des dictionnaires
				suitePriorDic
			)?			
		)?
	PD
	POINT
	////// --- à compléter pour chargement --- //////
	;
	
// suiteRdict:
suiteDicInit:
		//vide
	|
		VIRG id=Litteral suiteDicInit
	////// --- à compléter pour chargement --- //////
	;
	
controle_DU:
//	ctrl=(NN|NU|DN|DU)
// remplacement par ce qui suit et mise en commentaire dans Lex.g des tokens concerne's.
	ctrl=Litteral
	////// --- à compléter pour chargement --- //////
	;
	
// nouveau
suitePriorDic:
	|
		VIRG id=Litteral suitePriorDic
	////// --- à compléter pour chargement --- //////
	;

sens_NI:
	sens=Litteral
	////// --- à compléter pour chargement --- //////
	;

// 17/12/2015:
// Modification de l'appel à la partie droite qui pour la règle motinc est réduite aux
// parties action, modification d'entrée et sous-règles.
motinc:
	id=MOTINC
	DXPTS
	pg_rgl
	EG EG
//	pd_rgl
	p_act?
	
	(
		SLASH SLASH p_me ? 
		(SLASH SLASH p_asr? )? 
	)?	
	
	POINT
	;

// partie gauche, suite de formats
// *******************************
pg_rgl	:
	idf=Litteral
	(
		MOINSMOINS 
		idf2=Litteral
	)* ;


// partie droite
pd_rgl:
	p_act?
	(
		SLASH p_cond?
		(
			SLASH p_me?
			(
				SLASH p_cs?				// à régler comme les conditions sauf origine: cf doc
				(SLASH p_asr? )?		// à régler comme les formats
			)?
		)?
	)? ;

// Action: affectations et appels aux fonctions spéciales
p_act:
	act_el
	( 
		VIRG act_el
	)*
	;

// Action élémentaire
// Appelé par p_act
// Appelle mg_aff, md_aff, or1, fonc.
//
act_el :
	(
		mg_aff
		DXPTS EG 
		md_aff
	|
		eELIM=ELIM 
		PG
			o1=or1
			(
				VIRG o2=or1
			)*
		et=PD
	| 
		eELIT=ELIT
		PG 
			o=or1
		PD
	|
		fonc
	)
;

// Appelé par exbool_ATEF
b:
	c
	(
	// rien
	|	
		ET b
	)
	;

// Appelé par b	
c:
// 09/12/2009: A revoir, à cause de "rule c has non-LL(*) decision".
	(
		relation_ATEF
	|
		NO c
	|
		PG exbool_ATEF
		PD
	)
	;

// Élément de partie modification d'entrée.
// ****************************************
// Appelé par p_me.	
// Appelle origineCH, aSubstituer, Chaine
elt_p_me :
	etiq=TCHAINE	// Transformation de CHAINE
	PG
		orig=origineCHr	// origine dans C de la recherche: [$]Entier
		VIRG 
			psdch=chaineARemplacer 	// pseudo-chaîne à remplacer: longueur | * | chaîne
		VIRG 
			ch=Chaine 		// chaîne de remplacement (litteral p64)		
	PD
	
		// Pour avoir le nd OrigineLongueur en fils gauche du nd TCHAINE, on le crée
		// seulement ici afin qu'il soit le premier dépilé et donc affecté en fils gauche
		// du nd TCHAINE.
	;

// Appelé par elt_p_me
chaineARemplacer :
		mu=MULT
	|	
		(DOL)? // dollar = signe -
		ent=Litteral // entier		
	|
		ch=Chaine // 'litteral' p64
	;

// Partie sous-règles:
// Appelé par pd_rgl et par sousGrammaires
// Appelle rp_asr
p_asr:
	id=Litteral /* nom de règle */
	rp_asr
	;

// Appelé par pd_rgl (partie droite de règle)
p_cond :
	exbool_ATEF
	;

// 'condition S': la condition sur le suivant est analogue à la partie condition	sauf
// pour l'origine, A, T, R et les PSi sont exclus.
p_cs :
	exbool_ATEF
	;

// Partie modification de l'entrée
// *******************************
// Appelé par motinc et pd_rgl	
p_me:
	elt_p_me
	(VIRG elt_p_me)*
	; // p_me
	
// Appelée par p_asr
rp_asr:
		// vide
	|
		VIRG id=Litteral
		rp_asr
	; // rp_asr

// Appelé par p_cond et p_cs	
exbool_ATEF :
	b
	(
		// vide
	|
		OU exbool_ATEF
	)
	; // exbool_ATEF

// Appelé par act_el	
fonc:
	etiq=(FINAL|ARRET|STOP|TRANS|TRANSA|INIT|SOL
	|SEC
	|ARF|ARD|IDN
	|IDX|ISN|ISX|IFN|IFX|IHN|IHX|IAN|IAX|DST)
	;

// membre gauche d'une affectation
// Appelé par act_el
// Appelle op
mg_aff :
	op
;

// membre droit d'une affectation, appelé par act_el
md_aff :
	d ;

// Appelé par c.
relation_ATEF :
	(
	// TOURN n'est pas un token réservé (plus dans Lex) ?
		TOURN PG or1 PD
	|
		//jc: pour faire comme Jacques Chauché
		op
		(
			(UNION | INTER)		
			d1=d
		)?
		
		rel=relateur_ATEF 
		
		d2=d
	)
; // relation_ATEF

relateur_ATEF:
		ee=E
	|
		ne=NE
	|
		da=DANS
	|
		in=INC
; // relateur_ATEF

// Appelé par relation_ATEF
// membre droit d'une affectation: expression ensembliste
d :
	re=e
	(	
		// vide
	|
		UNION rd=d
	)
;

// Appelé par d
e :
	//	op				// Dans la grammaire d'origine, mais ambigü !
	//	|

	rf=f
	(
		// vide
	|	
		INTER e
	)
; // e

// Appelé par e
f :
	//		NO f		// Il s'agit, ici, plutôt du complément ? 
	// Et de toutes façons pb avec la grammaire si on laisse NO.
		COMPL f
	|
		PG 
		d
		PD
	|
		ro=op
; // f

// Appelé par mg_aff et md_aff via d, e, f.
// Appelle opvg, opdu, orr, origineCH, longueurCH.
// page 72:	
op :
// typevar = EXC ou NEX, nom de variable ou de valeur (y compris 'valeur0')
// ou typevar = VG
// ou typevar = DICT
// ou typevar = UL
// Les actions cachent les prédicats !!!	D'où suppression de: { dtr("op", trgr);}
			og=opvg			// VAR, VAREM,...
			
			PG 
			orr
			PD			
/*
//JC: 17/10/16: suppression des mots-clés DICT, UL, DICT0, UL0 cf Lex.g4.
		|
			ou=opdu			// opérande DICT, UL,...			
			PG orr//c:[end, t] 
			PD
			//c:	{ creerNdBin2($ou.n, (char*) $ou.s.data()); }
			{ creerNoeudBinaire2($ou.n, $ou.s); }
*/
		|			
			// mettre ici le prédicat end == cd ou cs ?
			etiq=SCHAINE		
			
			PG 		
				orr
				VIRG
				origineCH				
				VIRG 
				longueurCH				
			PD
		|
			id=Litteral // nom de variable exclusive ou ensembliste
			PG 
				orr
			PD
	  |
		|
		l=Litteral
			(
					// vide 
				|
					PG orr
					PD
			)			
		|
			ch=Chaine
	; // op

// Origine, appelé par op.
// 'or' est réservé en ANTLR et ne peut donc être employé.	
// À la postcompilation de tester la correction de la valeur de l'origine qu'on lit
// comme un littéral mais dont le type de nœud le représentant sera de type Origine
orr :

	/*
	// remplacement des mots-clés ci-dessous par une suite de caractères à tester.
		(
	//		{ end==cd && typevar==lit }?
			(A|C|S|P1|P2|P3|P4)
		|
			{ end==cs && typevar==lit }?
			(C|S|P1|P2|P3)
		|
			{ end==ga && typevar==ul }?
			(C|S)
		|
			{ end==da && typevar==ul }?
			(A|T|C)
		|
			{ end==cs }?
			(C|S|R|P1|P2|P3)
		|
			{ end==cd }?
			(A|C|R|P1|P2|P3|P4|PS1|PS2|PS3|PS4|PS5|PS6|PS7|PS8|PS9)
		)
	*/
	l=Litteral
; // orr
		
// origineCH p64
// Appelé par op et elt_p_me.
origineCH
	:
	(DOL)?
	 
	etiq=Litteral
; // origineCH
	 
origineCHr :
	(
		DOL
	)? 
	etiq=Litteral ;


// longueur: $nombre | nombre | *
// Appelé par op
longueurCH :
		(DOL)?
		etiq=Litteral
	|
		mu=MULT
; // longueurCH

// Appelé par op, la valeur numérique ANTLR du token est retournée. 
opvg :
		etiq=(VAR|VAREM|VARES|VARM|VARNM|VARNS|VARS)
		; // opvg

// or1 et "les or" (cf page 73) à distinguer avec des attributs:	
or1 :
	// remplacement des mots-clés ci-dessous par une suite de caractères a`tester.
	//	(C|S|P1|P2|P3|P4)
	etiq=Litteral
; // or1

// partie -SGRAM-, ajoutée le 10/06/2015
sousGrammaires:
	SGRAM				// "-SGRAM-"
	Litteral
	DXPTS
	p_asr?
	POINT
; // sousGrammaires
