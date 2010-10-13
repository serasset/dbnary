------------ extract-terme
usage : extract-terme [-f | -e | -d] [file]
-f : french  -e : english  -d : deutsch / la langue du fichier doit correspondre

--> crée la liste des termes de la langue dans le fichier file.terme
(dernière ligne du fichier : nombre de termes extraits)

------------ extract-def
usage : extract-def [file]

--> crée la liste des définitions extraites dans le fichier file.def
-->	le fichier file.def.hist donne les fréquences des différents mots dans les définitions 

------------ extract-nbsens
usage : extract-nbsens [-f | -e | -d] [file]
-f : french  -e : english  -d : deutsch / la langue du fichier doit correspondre

--> le fichier file.nbsens donne le degré de polysémie de chacun des termes définis (ordre décroissant)

------------ extract-defNoyau
usage : extract-defNoyau [-f | -e | -d] [file]
-f : french  -e : english  -d : deutsch / la langue du fichier doit correspondre

--> le fichier file.Noyau donne toutes les définitions (-D-) d'un même terme (-O-)

# format d'entrée : Gilles
# sorties : file.Noyau : toutes les définitions d'un terme (non trié)