#!/bin/bash
#$ -j y -S /bin/sh

# Pour fonctionner il faut respecter la structure suivante:
#=> Repertoire de travail
#      |
#      |-input  (nom du jdd)%
#      |-inputg (nom du jdd graphique)
#      |-v25_1.RESTART (ou v25_2.RESTART pour une reprise)
#      |-*.f           (masques de calcul)
#      |-reader        (repertoire contenant les masques de reader)
#
# le symbole % indique les donnees obligatoires a fournir
#
#  si le jdd s'appelle "input"
#  le jdd graphique doit s'appeler "inputg"
#  les masques de calcul doivent etre places au meme niveau que le jdd
#  les masques de reader doivent etre places dans le repertoire "reader"
#   situe au meme niveau que le jdd ou bien etre nommes *.reader.
#   Ils seront alors renommes (supp. de l'extension.reader) puis deplaces dans le rep. reader
#  pour lancer un calcul a partir d'une reprise, il suffit de placer le
#   fichiers v25_1.RESTART (ou v25_2.RESTART) au niveau du jdd

# Declaration du repertoire contenant les executables
#  export VERS=/cluster/appli/cathare/v25_1-g77-3.2.2/mod2.1/
#  export v25_1=/cluster/appli/cathare/v25_1-g77-3.2.2/mod2.1/

export VERS="/opt/CATHARE2_V25_3_MOD931"
echo "VERSION: "$VERS

export v25_1=$VERS
export v25_2=$VERS
export v25_3=$VERS

export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:$VERS"/"$lib

# le premier argument du script est le fichier input (inputg est automatiquement suppose present)
input=$1
cwd=`pwd`

if [ "$pid""zz" == "zz" ] ; then
  pid=$cwd/PID
fi

# process number in "PID" file
echo $$ >> $pid

echo "Initialisation..."
# convert to unix (EoL issue)
dos2unix *

# Renommage des reader si besoin
readers=`ls reader_*`
if [ "$readers""zz" != "zz" ] ; then
  mkdir -p reader
  for i in $readers ; do
    j=`echo $i | cut -d_ -f2`
    mv $i reader/$j
  done
fi

echo "Lancement du reader..."
# Lancement du reader avec gestion des masques de reader
if [ -d "reader" ] ; then
  sh $VERS/unix-procedur/vers.unix reader &
    PID=$!
    echo $PID >> $pid
    wait $PID
  sh $VERS/unix-procedur/read.unix $input mask > reader.listing &
    PID=$!
    echo $PID >> $pid
    wait $PID
  echo "  execution avec mask reader"
  sh $VERS/unix-procedur/vers.unix reader &
    PID=$!
    echo $PID >> $pid
    wait $PID
else
  sh $VERS/unix-procedur/vers.unix &
    PID=$!
    echo $PID >> $pid
    wait $PID
  sh $VERS/unix-procedur/read.unix $input > reader.listing &
    PID=$!
    echo $PID >> $pid
    wait $PID
  echo "  execution sans mask reader"
  sh $VERS/unix-procedur/vers.unix &
    PID=$!
    echo $PID >> $pid
    wait $PID
fi

# Recherche d'erreur au niveau du reader
if [ `grep ERROR reader.listing|wc -w` != 0 ] ; then
  echo "Erreur reader !"
  cd $cwd
  exit -1
else
  echo "  pas d'erreur reader"
fi

# Traitement calcul initial permanent si besoin
LS_NO1=`ls -I $1 -I PILOT.f -I reader.listing -F | grep -v "@" | cut -d':' -f1 | uniq | tr '\n' ' '`
PERMINIT=`grep PERMINIT $LS_NO1 | cut -d':' -f1 | uniq`
PERMINIT=`echo $PERMINIT | tr -d '\n'`

if [ ! "$PERMINIT""zz" == "zz" ] ; then
  echo "Calcul permanent initial: "$PERMINIT
  sh $VERS/unix-procedur/read.unix $PERMINIT > perminit.listing &
    PID=$!
    echo $PID >> $pid
    wait $PID
  sh $VERS/unix-procedur/cathar.unix > perm.listing &
  PERMINIT_POSTPRO=`grep CHRONO *$PERMINIT* | cut -d: -f1 | uniq`
  if [ ! "$PERMINIT_POSTPRO""zz" == "zz" ] ; then
    echo "Postpro permanent initial"
    sh $VERS/unix-procedur/postpro.unix $PERMINIT_POSTPRO > perminit_postpro.listing &
      PID=$!
      echo $PID >> $pid
      wait $PID
  fi
else
  echo "Pas de calcul permanent initial"
fi  

echo "Lancement de Cathare..."
# Lancement de Cathare
sh $VERS/unix-procedur/cathar.unix > listing &
  PID=$!
  echo $PID >> $pid
  wait $PID

# Recherche d'erreur pendant le calcul
if [ `grep " NORMAL END OF CATHAR RUN" listing|wc -l` != 1 ] ; then
# les calculs ont plante
  echo "Erreur calcul !"
  cd $cwd
  exit 1
else
  echo "  pas d'erreur calcul"
fi

echo "Lancement du post-traitement..."
# Lancement du post-traitement
POSTPRO=`grep CHRONO *$1* | cut -d: -f1 | uniq`
if [ ! "$POSTPRO""zz" == "zz" ] ; then
  echo "Lancement du post-traitement..."
  sh $VERS/unix-procedur/postpro.unix $POSTPRO > postpro.listing &
    PID=$!
    echo $PID >> $pid
    wait $PID
else
  echo "(!!!) Pas de post-traitement..."
fi

# Recherche d'erreur pendant le post-traitement
if [ `grep "ERROR" postpro.listing|wc -l` != 0 ] ; then
# le postpro a plante
  echo "Erreur post-pro !"
  cd $cwd
  exit 2
else
  echo "  pas d'erreur post-pro"
fi

# Menage
rm *.f
rm *.H
rm *.exe
rm *.unix
rm DICO
rm FORT21
rm *.INIT
rm *.STOP
rm *.SUIVI

# Suppress "PID" file
if [ -f $pid ]; then
	rm -f $pid
fi