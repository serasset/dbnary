/*
 * Creating vartrans relation from dbnary:Translation instances, in the case 
 * where target translation has a unique lexical entry
 */
use DB;

create procedure create_vartrans_links (
  in vartrans_graph varchar := 'http://kaiko.getalp.org/dbnary/vartrans', 
  in batch_size integer := 100000
  )
{
  declare qr varchar;
  declare qstate, msg, descs, translations, translation any;
  declare translatable_as any ;
  declare vartrans_triples any ;
  declare cursor_handle any ;
  declare cnt, b integer ;
  
  log_message (	'VARTRANS: Selecting translations...\n' ) ;
  qstate := '00000';
  qr := 'SPARQL SELECT (sample(?sle) as ?sle), (sample(?le) as ?tle) WHERE {
      ?trans
        a dbnary:Translation ;
        dbnary:isTranslationOf ?sle ;
        dbnary:targetLanguage ?lg ;
        dbnary:writtenForm ?wf.
      ?sle a ontolex:LexicalEntry;
        lexinfo:partOfSpeech ?pos.
      ?le a ontolex:LexicalEntry;
        dcterms:language ?lg;
        rdfs:label ?wf;
        lexinfo:partOfSpeech ?pos.
      FILTER (REGEX(STR(?le), "^http://kaiko.getalp.org/dbnary/.../[^_]")) .
      } GROUP BY ?trans
        HAVING (COUNT(*) = 1) LIMIT 20000' ;
  translations := null ;
  exec (qr, qstate, msg, vector (), 0, null, translations, cursor_handle);

  if (qstate <> '00000')
    signal (qstate, msg);

  log_message (	'VARTRANS: Iterating over translations.' ) ;
  cnt := 0; b := 0;
  translatable_as := iri_to_id ('http://www.w3.org/ns/lemon/vartrans#translatableAs') ;
  vartrans_triples := vector () ;
  while (0 = exec_next (cursor_handle, null, null, translation)) {
    vartrans_triples := vector_concat(vartrans_triples, vector (vector (translation[0], translatable_as, translation[1]))) ;
    cnt := cnt + 1 ; b := b + 1 ;
    if ( b = batch_size ) {
      log_message (	sprintf('VARTRANS: Inserting Batch (cnt = %d).', cnt) ) ;
      DB.DBA.RDF_INSERT_TRIPLES (vartrans_graph, vartrans_triples);
      vartrans_triples := vector () ;
      b := 0 ;
    }
  }
  exec_close (cursor_handle);
  
  DB.DBA.RDF_INSERT_TRIPLES (vartrans_graph, vartrans_triples);

  return cnt ;
}

create procedure create_vartrans_links2 (
  in vartrans_graph varchar := 'http://kaiko.getalp.org/dbnary/vartrans', 
  in batch_size integer := 100000
  )
{
  declare qr varchar;
  declare qstate, msg, descs, translations, translation any;
  declare translatable_as any ;
  declare vartrans_triples any ;
  declare cursor_handle any ;
  declare cnt, b integer ;
  
  log_message (	'VARTRANS: Selecting translations...\n' ) ;
  qr := 'SPARQL SELECT ?sle, ?lg, ?wf WHERE {
      ?trans
        a dbnary:Translation ;
        dbnary:isTranslationOf ?sle ;
        dbnary:targetLanguage ?lg ;
        dbnary:writtenForm ?wf.
      ?sle a ontolex:LexicalEntry;
        } LIMIT 1000' ;

  qstate := '00000';

  translations := null ;
  exec (qr, qstate, msg, vector (), 0, descs, translations, cursor_handle);

  if (qstate <> '00000')
    signal (qstate, 'Problem: ' || msg);

  log_message (	'VARTRANS: Iterating over translations.' ) ;
  cnt := 0; b := 0;
  translatable_as := iri_to_id ('http://www.w3.org/ns/lemon/vartrans#translatableAs') ;
  vartrans_triples := vector () ;
  while (0 = exec_next (cursor_handle, null, null, translation)) {
    vartrans_triples := vector_concat(vartrans_triples, vector (vector (translation[0], translatable_as, translation[0]))) ;
    cnt := cnt + 1 ; b := b + 1 ;
    if ( b = batch_size ) {
      log_message (	sprintf('VARTRANS: Inserting Batch (cnt = %d).', cnt) ) ;
      DB.DBA.RDF_INSERT_TRIPLES (vartrans_graph, vartrans_triples);
      vartrans_triples := vector () ;
      b := 0 ;
    }
  }
  exec_close (cursor_handle);
  
  DB.DBA.RDF_INSERT_TRIPLES (vartrans_graph, vartrans_triples);

  return cnt ;
}

create procedure drop_vartrans_graph() {
  SPARQL drop SILENT graph <http://kaiko.getalp.org/dbnary/vartrans> ;
}


create procedure purge_vartrans_links() {
  SPARQL delete {?s vartrans:translatableAs ?o} FROM <http://kaiko.getalp.org/dbnary/vartrans> WHERE {?s vartrans:translatableAs ?o} ;
}