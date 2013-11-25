select update_graph(id) from kb where name = ':ontName';

select update_equivalent_class_group(id) from kb where name = ':ontName';

select infer_subclass_intersect(id) from kb where name = ':ontName';

select infer_inheritable_property_on_class(kb.id,n.rid ) from kb, graph_nodes n, resourcetype rt
where kb.name = ':ontName' and n.kbid = kb.id and rt.name = 'property' 
   and rt.id = n.rtid and lower(n.label)= 'part_of';

select update_inference_edges(id) from kb where name = ':ontName';

select * from create_dag_index(':ontName', 'has_part', true, false);

select * from create_dag_index(':ontName', 'subClassOf', false, false);

select compute_dag_neighbor_count(m.id) from dag_index_metadata m, kb k where m.kbid = k.id and k.name = ':ontName';

-- for iDash pathway ontologies
--select set_biopax_labels(id) from kb where name = ':ontName';

select fill_term_category(id, '''Anatomical object'', ''brain'', ''Cell'', ''Device'', ''Cellular Component'', ''extracellular structure'', ''cell line'', ''tissue section'', ''molecular entity'', ''Site'', ''Institution'', ''Platform'', ''Population'', ''Disease'', ''Biological_region'', ''gene'', ''Molecule role'', ''Drug'', ''Data object'', ''Assay'', ''Organism'', ''Data role'', ''Chemical role'', ''Reagent role'', ''familial role'', ''cell role'' , ''Quality'', ''Biomaterial_region'', ''Artifact Object'', ''Phenotype'', ''age'', ''Process'', ''behavioral process'', ''biological_process'', ''Regional Part Of Cell'', ''Resource''', '''sao1813327414'', ''birnlex_11013'', ''Function'', ''birnlex_2'', ''birnlex_2087'', ''nlx_res_20090101'', ''birnlex_6'', ''CHEBI_23367'', ''birnlex_11021''') from kb where name = ':ontName';

--select fill_nif_cards();

update kb set name = ':oldName' where name = ':finalName';

update kb set name = ':finalName' where name = ':ontName';

--select delete_kb(':oldName');