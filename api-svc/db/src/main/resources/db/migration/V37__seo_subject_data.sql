-- Step 1: Make schema_id NOT NULL (required for primary key)
ALTER TABLE raido.api_svc.subject_type
    ALTER COLUMN schema_id SET NOT NULL;

-- Step 2: Drop the foreign key from raid_subject that references subject_type
ALTER TABLE raido.api_svc.raid_subject
    DROP CONSTRAINT raid_subject_subject_type_id_fkey;

-- Step 3: Add schema_id column to raid_subject
ALTER TABLE raido.api_svc.raid_subject
    ADD COLUMN subject_type_schema_id INTEGER;

-- Step 4: Populate the new column from existing relationships
UPDATE raido.api_svc.raid_subject rs
SET subject_type_schema_id = st.schema_id
FROM raido.api_svc.subject_type st
WHERE rs.subject_type_id = st.id;

-- Step 5: Make the new column NOT NULL
ALTER TABLE raido.api_svc.raid_subject
    ALTER COLUMN subject_type_schema_id SET NOT NULL;

-- Step 6: Drop the existing primary key on subject_type
ALTER TABLE raido.api_svc.subject_type
    DROP CONSTRAINT subject_pkey;

-- Step 7: Create the new composite primary key
ALTER TABLE raido.api_svc.subject_type
    ADD CONSTRAINT subject_type_pkey PRIMARY KEY (id, schema_id);

-- Step 8: Re-create the foreign key as a composite reference
ALTER TABLE raido.api_svc.raid_subject
    ADD CONSTRAINT raid_subject_subject_type_fkey
        FOREIGN KEY (subject_type_id, subject_type_schema_id)
            REFERENCES raido.api_svc.subject_type (id, schema_id);

-- Step 9: Add foreign key from raid_subject.subject_type_schema_id to subject_type_schema
ALTER TABLE raido.api_svc.raid_subject
    ADD CONSTRAINT raid_subject_subject_type_schema_id_fkey
        FOREIGN KEY (subject_type_schema_id)
            REFERENCES raido.api_svc.subject_type_schema (id);

alter table raido.api_svc.subject_type_schema add column id_starts_with varchar;

insert into raido.api_svc.subject_type_schema (uri, status, id_starts_with)
values ('https://vocabs.ardc.edu.au/viewById/317', 'active', 'https://linked.data.gov.au/def/anzsrc-seo/2020/');

update raido.api_svc.subject_type_schema set id_starts_with = 'https://linked.data.gov.au/def/anzsrc-for/2020/' where id = 2;
update raido.api_svc.subject_type_schema set id_starts_with = 'https://linked.data.gov.au/def/anzsrc-for/2020/' where id = 3;

insert into raido.api_svc.subject_type (id, name, description, note, schema_id)
select id, name, description, note, 2
from raido.api_svc.subject_type
where schema_id = 1;

insert into raido.api_svc.subject_type (id, name, description, note, schema_id)
select id, name, description, note, 3
from raido.api_svc.subject_type
where schema_id = 1;

insert into raido.api_svc.subject_type (id, name, description, note, schema_id) values
('10','ANIMAL PRODUCTION AND ANIMAL PRIMARY PRODUCTS','This division covers R&D directed towards breeding and farming livestock, and the production of associated primary livestock products.','a) Cereal crops grown for both grain and fodder or for feed grain or fodder alone are included in Division 26 Plant production and plant primary products.
b) The manufacture of processed animal products, including fish, and their primary products is included in the appropriate groups in Division 24 Manufacturing.
c) Transportation of live animals or animal products is included in Division 27 Transport.
d) Operation of irrigation systems and the provision of waste handling services such as collection, storage, recycling and disposal provided by local councils or private or non-profit waste management enterprises are included in Group 1105 Water and waste services.
e) Air quality; control of pests, diseases and exotic species; ecosystem assessment and management; flora, fauna and biodiversity; land and water management; rehabilitation of degraded areas; remnant vegetation and conservation areas; soils; and natural hazards in agricultural environments are included in Divisions 18 Environmental management and 19 Environmental policy, climate change and natural hazards.',4),
('1001','Environmentally sustainable animal production','This group covers R&D directed towards environmentally sustainable production of animals and animal primary products by minimising the generation of emissions and waste, and by reducing or optimising the consumption of water and other natural resources.','a) Minimising input waste (other than water and energy) is to be categorised according to the objective that benefits from the improvement of input use.
b) Waste handling services such as collection, storage, recycling and disposal provided by local councils or private or non-profit waste management businesses are included in Group 1105 Water and waste services.
c) Air quality; control of pests, diseases and exotic species; ecosystem assessment and management; flora, fauna and biodiversity; land and water management; rehabilitation of degraded areas; remnant vegetation and conservation areas; soils; and natural hazards in agricultural environments are included in Divisions 18 Environmental management and 19 Environmental policy, climate change and natural hazards.',4),
('100101','Management of gaseous waste from animal production (excl. greenhouse gases)','','',4),
('100102','Management of liquid waste from animal production (excl. water)','','',4),
('100103','Management of solid waste from animal production','','',4),
('100104','Management of water consumption by animal production','','',4),
('100199','Environmentally sustainable animal production not elsewhere classified','','',4),
('1002','Fisheries - aquaculture','This group covers R&D directed towards improving the characteristics and harvesting of fisheries grown or farmed in aquaculture either for commercial or other reasons.','a) Improving the characteristics and harvesting of fisheries caught in the wild is included in Group 1003 Fisheries - wild caught.
b) Preparation, handling and storage of unprocessed or minimally processed fish is included in Group 1006 Primary products from animals.
c) Fish welfare and fish products traceability and quality assurance are included in Group 1099 Other animal production.
d) Manufacture of processed fish and seafood products and their primary products is included in Group 2413 Processed food products and beverages (excl. dairy products).',4),
('100201','Aquaculture crustaceans (excl. rock lobster and prawns)','','',4),
('100202','Aquaculture fin fish (excl. tuna)','','',4),
('100203','Aquaculture molluscs (excl. oysters)','','',4),
('100204','Aquaculture oysters','','',4),
('100205','Aquaculture prawns','','',4),
('100206','Aquaculture rock lobster','','',4),
('100207','Aquaculture tuna','','',4),
('100299','Fisheries - aquaculture not elsewhere classified','','',4),
('1003','Fisheries - wild caught','This group covers R&D directed towards improving the characteristics and harvesting of fisheries caught in the wild for commercial or recreational reasons.','a) Improvement of the characteristics and harvesting of aquaculture fisheries is included in Group 1002 Fisheries - aquaculture.
b) Preparation, handling and storage of unprocessed or minimally processed fisheries products is included in Group 1006 Primary products from animals.
c) Fish welfare and fish products traceability and quality assurance are included in Group 1099 Other animal production.
d) Manufacture of processed fish and seafood products and their primary products is included in Group 2413 Processed food products and beverages (excl. dairy products).',4),
('100301','Fisheries - recreational freshwater','','',4),
('100302','Fisheries - recreational marine','','',4),
('100303','Wild caught crustaceans (excl. rock lobster and prawns)','','',4),
('100304','Wild caught edible molluscs','','',4),
('100305','Wild caught fin fish (excl. tuna)','','',4),
('100306','Wild caught prawns','','',4),
('100307','Wild caught rock lobster','','',4),
('100308','Wild caught tuna','','',4),
('100399','Fisheries - wild caught not elsewhere classified','','',4),
('1004','Livestock raising','This group covers R&D directed towards farming or breeding livestock. It includes beekeeping and pet breeding.','a) Preparation, handling, and storage of primary livestock products, e.g. unprocessed milk, raw wool, raw hides and skins, is included in Group 1006 Primary products from animals.
b) Animal welfare and livestock product traceability and quality assurance are included in Group 1099 Other animal production.
c) Manufacture of processed livestock products, e.g. processed milk and processed skins, is included in Division 24 Manufacturing.',4),
('100401','Beef cattle','','',4),
('100402','Dairy cattle','','',4),
('100403','Deer','','',4),
('100404','Game livestock (e.g. kangaroos, wallabies, camels, buffaloes)','','',4),
('100405','Goats','','',4),
('100406','Horses','','',4),
('100407','Insects','','',4),
('100408','Minor livestock (e.g. alpacas, ostriches, crocodiles, farmed rabbits)','','',4),
('100409','Non-cattle dairy','','',4),
('100410','Pigs','','',4),
('100411','Poultry','','',4),
('100412','Sheep for meat','','',4),
('100413','Sheep for wool','','',4),
('100499','Livestock raising not elsewhere classified','','',4),
('1005','Pasture, browse and fodder crops','This group covers R&D directed towards growing sown legumes, sown grasses, browse and fodder crops, native vegetation and any other pasture crops. It includes R&D directed towards harvesting, preparation and storage of pasture, browse and fodder crops.','a) Growing grain, seed, industrial and horticultural crops, including cereals grown either partially or entirely for use as fodder or feed grain is included in Division 26 Plant production and plant primary products.
b) Operation of irrigation systems is included in Group 1105 Water and waste services.',4),
('100501','Browse crops','','',4),
('100502','Lucerne','','',4),
('100503','Native and residual pastures','','',4),
('100504','Non-cereal crops (non-cereal crops for hay/silage/green feed)','','',4),
('100505','Sown pastures (excl. lucerne)','','',4),
('100599','Pasture, browse and fodder crops not elsewhere classified','','',4),
('1006','Primary products from animals','This group covers R&D directed towards improving the preparation, handling or storage of primary animal products such as eggs, honey, raw wool, unprocessed or minimally processed fish or milk, and other animal fibres, hides and skins.','a) Improving the characteristics and harvesting of fisheries is included in Groups 1002 Fisheries - aquaculture and 1003 Fisheries - wild caught.
b) Raising livestock from which these products are obtained is included in Group 1004 Livestock raising.
c) Animal welfare is included in Group 1099 Other animal production.
d) Manufacture of processed products from primary animal products is included in Division 24 Manufacturing.
e) Transportation of live animals or animal products is included in Division 27 Transport.',4),
('100601','Eggs','','',4),
('100602','Honey','','',4),
('100603','Pearls','','',4),
('100604','Raw wool','','',4),
('100605','Unprocessed or minimally processed fish','','',4),
('100606','Unprocessed or minimally processed milk','','',4),
('100699','Primary products from animals not elsewhere classified','','',4),
('1099','Other animal production and animal primary products','This group covers R&D directed towards animal production not elsewhere classified.','a) Cereal crops grown for both grain and fodder or for feed grain or fodder alone are included in Division 26 Plant production and plant primary products.
b) The manufacture of processed animal products, including fish, and their primary products is included in the appropriate groups in Division 24 Manufacturing.
c) Transportation of live animals or animal products is included in Division 27 Transport.
d) Operation of irrigation systems and the provision of waste handling services such as collection, storage, recycling and disposal provided by local councils or private or non-profit waste management enterprises are included in Group 1105 Water and waste services.
e) Air quality; control of pests, diseases and exotic species; ecosystem assessment and management; flora, fauna and biodiversity; land and water management; rehabilitation of degraded areas; remnant vegetation and conservation areas; soils; and natural hazards in agricultural environments are included in Divisions 18 Environmental management and 19 Environmental policy, climate change and natural hazards.',4),
('109901','Animal adaptation to climate change','','',4),
('109902','Animal welfare','','',4),
('109903','Fish product traceability and quality assurance','','',4),
('109904','Livestock product traceability and quality assurance','','',4),
('109999','Other animal production and animal primary products not elsewhere classified','','',4),
('11','COMMERCIAL SERVICES AND TOURISM','This division covers R&D directed towards the provision of commercial services to the economic and social development sectors.','a) Security services or surveillance systems for military purposes is included in Division 14 Defence; and for corrective purposes is included in Group 2304 Justice and the law.
b) Management of energy consumption from commercial services and tourism is included in Group 1701 Energy efficiency.
c) Management of solid, liquid or gaseous waste generated as an integral process within an industry to minimise or prevent impacts on environment is included in respective industry divisions (e.g. Division 24 Manufacturing) and groups (e.g. Group 2406 Environmentally sustainable manufacturing activities).
d) Provision of construction services, including architectural and building management services and construction of water infrastructure, are included in Division 12 Construction.
e) Development of transport infrastructure and transport and freight services are included in Division 27 Transport.
f) Provision of information, communications and computing services, including media services, is included in Division 22 Information and communication services.
g) Issues relating to international trade are included in Group 1501 International trade policy.
h) Calibration services are included in Group 1504 Measurement standards and calibration services.
i) Music and theatre production, sport and services to sport activities (including sporting clubs), and recreational and amusement services mainly of a non-commercial nature (e.g. parks, botanical gardens) is included in Division 13 Culture and society.
j) Air quality; control of pests, diseases and exotic species; ecosystem assessment and management; flora, fauna and biodiversity; land and water management; rehabilitation of degraded areas; remnant vegetation and conservation areas; soils; and natural hazards in environments affected by commercial services and tourism are included in Divisions 18 Environmental management and 19 Environmental policy, climate change and natural hazards.',4),
('1101','Environmentally sustainable commercial services and tourism','','',4),
('110101','Management of gaseous waste from commercial services and tourism (excl. greenhouse gases)','','',4),
('110102','Management of liquid waste from commercial services and tourism (excl. water)','','',4),
('110103','Management of solid waste from commercial services and tourism','','',4),
('110104','Management of water consumption by commercial services and tourism','','',4),
('110199','Environmentally sustainable commercial services and tourism not elsewhere classified','','',4),
('1102','Financial services','','',4),
('110201','Finance services','','',4),
('110202','Investment services (excl. superannuation)','','',4),
('110203','Superannuation and insurance services','','',4),
('110299','Financial services not elsewhere classified','','',4),
('1103','Property, business support services and trade','This group covers R&D directed towards the development and provision of property, business support services and trade.','a) Security services or surveillance systems for military purposes is included in Division 14 Defence; and for corrective purposes is included in Group 2304 Justice and the law.
b) Construction services, including architectural and building management services, are included in Division 12 Construction.
c) Transport and freight services are included in Division 27 Transport.
d) Information, communications and computing services is included in Division 22 Information and communication services.
e) Issues relating to international trade are included in Group 1501 International trade policy.',4),
('110301','Administration and business support services','','',4),
('110302','Professional, scientific and technical services','','',4),
('110303','Property services (incl. security)','','',4),
('110304','Wholesale and retail trade','','',4),
('110399','Property, business support services and trade not elsewhere classified','','',4),
('1104','Tourism services','This group covers R&D directed towards the development and provision of tourism services.','a) Development of transport infrastructure is included in Division 27 Transport.
b) Hospitality and recreation services are included in Group 1199 Other commercial services and tourism.',4),
('110401','Economic issues in tourism','','',4),
('110402','Socio-cultural issues in tourism','','',4),
('110403','Tourism infrastructure development','','',4),
('110499','Tourism services not elsewhere classified','','',4),
('1105','Water and waste services','This group covers R&D directed towards the development and provision of water and waste services and utilities to households and industrial and commercial premises.','a) Provision of energy services and utilities is included in Group 1703 Energy storage, distribution and supply.
b) Industrial processing of wastes and scrap to manufacture new products is included in the appropriate groups in Division 24 Manufacturing.
c) Management of solid, liquid or gaseous waste generated as an integral process within an industry to minimise or prevent impacts on environment is included in respective industry divisions (e.g. Division 24 Manufacturing) and groups (e.g. Group 2406 Environmentally sustainable manufacturing activities).
d) Construction of dams, water mains and pumping stations is included in Group 1205 Construction processes.
e) Management of environmental impacts from water and waste services is included in Group 1101 Environmentally sustainable commercial services and tourism.',4),
('110501','Waste management services','','',4),
('110502','Waste recycling services','','',4),
('110503','Water recycling services (incl. sewage and greywater)','','',4),
('110504','Water services and utilities','','',4),
('110599','Water and waste services not elsewhere classified','','',4),
('1199','Other commercial services and tourism','This group covers R&D directed towards the development and provision of commercial services not elsewhere classified. It includes recreational services of a commercial nature (e.g. casinos) and hospitality services (e.g. motels, taverns and restaurants).','a) Security services or surveillance systems for military purposes is included in Division 14 Defence; and for corrective purposes is included in Group 2304 Justice and the law.
b) Management of energy consumption from commercial services and tourism is included in Group 1701 Energy efficiency.
c) Management of solid, liquid or gaseous waste generated as an integral process within an industry to minimise or prevent impacts on environment is included in respective industry divisions (e.g. Division 24 Manufacturing) and groups (e.g. Group 2406 Environmentally sustainable manufacturing activities).
d) Construction services, including architectural and building management services and construction of water infrastructure, are included in Division 12 Construction.
e) Development of transport infrastructure and transport and freight services are included in Division 27 Transport.
f) Provision of information, communications and computing services, including media services, is included in Division 22 Information and communication services.
g) Issues relating to international trade are included in Group 1501 International trade policy.
h) Calibration services are included in Group 1504 Measurement standards and calibration services.
i) Music and theatre production, sport and services to sport activities (including sporting clubs), and recreational and amusement services mainly of a non-commercial nature (e.g. parks, botanical gardens) is included in Division 13 Culture and society.
j) Air quality; control of pests, diseases and exotic species; ecosystem assessment and management; flora, fauna and biodiversity; land and water management; rehabilitation of degraded areas; remnant vegetation and conservation areas; soils; and natural hazards in environments affected by commercial services and tourism are included in Divisions 18 Environmental management and 19 Environmental policy, climate change and natural hazards.',4),
('119901','Hospitality services','','',4),
('119902','Recreational services','','',4),
('119999','Other commercial services and tourism not elsewhere classified','','',4),
('12','CONSTRUCTION','This division covers R&D directed towards construction activities.','a) Energy conservation and efficiency in construction activities is included in Group 1701 Energy efficiency.
b) Manufacture of materials used in construction is included in the appropriate groups in Division 24 Manufacturing.
c) Transport planning unrelated to urban planning is included in Division 27 Transport.
d) Provision of communications services is included in Group 2201 Communication technologies, systems and services.
e) Property services (e.g. property developers, real estate agents, alarm and surveillance services) are included in Group 1103 Property, business support services and trade.
f) Waste handling services such as collection, storage, recycling and disposal provided by local councils or private or non-profit waste management businesses are included in Group 1105 Water and waste services.
g) Construction related environmental health issues are included in Group 2004 Public health (excl. specific population health).
h) Conservation of historic buildings and structures and heritage aspects of urban and regional planning are included in Group Group 1304 Heritage.
i) Air quality; control of pests, diseases and exotic species; ecosystem assessment and management; environmental and natural resource evaluation; environmental policy, legislation and standards; flora, fauna and biodiversity; land and water management; rehabilitation of degraded areas; remnant vegetation and conservation areas; soils; and natural hazards in urban and industrial environments or other environments affected by construction activities are included in Divisions 18 Environmental management and 19 Environmental policy, climate change and natural hazards.',4),
('1201','Building management and services','This group covers R&D directed towards the design, installation and maintenance of building services, as well as demolition and refurbishment.','a) Energy conservation and efficiency in building management and services is included in Group 1701 Energy efficiency.
b) Provision of communications services is included in Group 2201 Communication technologies, systems and services.
c) Development and provision of property services (e.g. property development, real estate and alarm and surveillance services) are included in Group 1103 Property, business support services and trade.
d) Conservation of historic buildings and structures is included in Group Group 1304 Heritage.',4),
('120101','Civil building management and services','','',4),
('120102','Commercial building management and services','','',4),
('120103','Industrial building management and services','','',4),
('120104','Institutional building management and services','','',4),
('120105','Residential building management and services','','',4),
('120199','Building management and services not elsewhere classified','','',4),
('1202','Construction design','This group covers R&D directed towards improving the activities associated with turning a project sketch plan into detailed architectural and engineering designs and specifications.','a) Energy conservation and efficiency other than design aimed at reducing the energy needs of finished buildings is included in Group 1701 Energy efficiency.',4),
('120201','Civil construction design','','',4),
('120202','Commercial construction design','','',4),
('120203','Industrial construction design','','',4),
('120204','Institutional construction design','','',4),
('120205','Residential construction design','','',4),
('120299','Construction design not elsewhere classified','','',4),
('1203','Construction materials performance and processes','This group covers R&D directed towards testing the performance, durability and life cycle of construction components and materials which are delivered to or assembled on construction sites.','a) Manufacture of industrial chemicals used in construction (e.g. paints, plastics) is included in Group 2409 Industrial chemicals and related products.
b) Manufacture of ceramics, glass and glass products and industrial mineral products including clay, cement and concrete products used in construction is included in Group 2403 Ceramics, glass and industrial mineral products.
c) Manufacture of basic metal products (e.g. copper wire, steel rods) used in construction is included in Group 2402 Basic metal products (incl. smelting, rolling, drawing and extruding).
d) Manufacture of fabricated metal products (e.g. aluminium frames, steel pipes, structural steel) used in construction is included in Group 2407 Fabricated metal products.',4),
('120301','Cement and concrete materials','','',4),
('120302','Glass materials','','',4),
('120303','Metals','','',4),
('120304','Polymeric materials and paints','','',4),
('120305','Stone, ceramics and clay materials','','',4),
('120306','Timber materials','','',4),
('120399','Construction materials performance and processes not elsewhere classified','','',4),
('1204','Construction planning','This group covers R&D directed towards improving the activities associated with the development of construction projects to the sketch plan stage.','a) Detailed architectural design is included in Group 1202 Construction design.
b) Transport planning unrelated to urban planning is included in Division 27 Transport.
c) Heritage aspects of urban and regional planning are included in Group Group 1304 Heritage.
d) Rural and urban land and water policy and evaluation is included in 1902 Environmental policy, legislation and standards.',4),
('120401','Civil construction planning','','',4),
('120402','Commercial construction planning','','',4),
('120403','Industrial construction planning','','',4),
('120404','Regional planning','','',4),
('120405','Residential construction planning','','',4),
('120406','Urban planning','','',4),
('120499','Construction planning not elsewhere classified','','',4),
('1205','Construction processes','This group covers R&D directed towards improving the processes and activities associated with translating plans and specifications into finished buildings and structures.','a) Energy conservation and efficiency in construction processes is included in Group 1701 Energy efficiency.',4),
('120501','Civil construction processes','','',4),
('120502','Commercial construction processes','','',4),
('120503','Industrial construction processes','','',4),
('120504','Institutional construction processes','','',4),
('120505','Residential construction processes','','',4),
('120599','Construction processes not elsewhere classified','','',4),
('1206','Environmentally sustainable construction activities','This group covers R&D directed towards environmentally sustainable ways of managing construction and building activities by minimising the generation of emissions and wastes and by reducing or optimising the consumption of water and other natural resources.','a) Minimisation of input waste (other than water and energy) is to be categorised according to the objective that benefits from the improvement of input use.
b) Energy conservation and efficiency in construction activities is included in Group 1701 Energy efficiency.
c) Design of buildings to minimise energy consumption in the finished building is included in Group 1202 Construction design.
d) Waste handling services such as collection, storage, recycling and disposal provided by local councils or private or non-profit waste management businesses are included in Group 1105 Water and waste services.
e) Air quality; control of pests, diseases and exotic species; ecosystem assessment and management; flora, fauna and biodiversity; land and water management; rehabilitation of degraded areas; remnant vegetation and conservation areas; soils; and natural hazards in urban and industrial environments or other environments affected by construction activities are included in Divisions 18 Environmental management and 19 Environmental policy, climate change and natural hazards.',4),
('120601','Management of gaseous waste from construction activities (excl. greenhouse gases)','','',4),
('120602','Management of liquid waste from construction activities (excl. water)','','',4),
('120603','Management of solid waste from construction activities','','',4),
('120604','Management of water consumption by construction activities','','',4),
('120699','Environmentally sustainable construction activities not elsewhere classified','','',4),
('1299','Other construction','This group covers R&D directed towards construction activities not elsewhere classified.','a) Energy conservation and efficiency in construction activities is included in Group 1701 Energy efficiency.
b) Manufacture of materials used in construction is included in the appropriate groups in Division 24 Manufacturing.
c) Transport planning unrelated to urban planning is included in Division 27 Transport.
d) Provision of communications services is included in Group 2201 Communication technologies, systems and services.
e) Property services (e.g. property developers, real estate agents, alarm and surveillance services) are included in Group 1103 Property, business support services and trade.
f) Waste handling services such as collection, storage, recycling and disposal provided by local councils or private or non-profit waste management businesses are included in Group 1105 Water and waste services.
g) Construction related environmental health issues are included in Group 2004 Public health (excl. specific population health).
h) Conservation of historic buildings and structures and heritage aspects of urban and regional planning are included in Group Group 1304 Heritage.
i) Air quality; control of pests, diseases and exotic species; ecosystem assessment and management; environmental and natural resource evaluation; environmental policy, legislation and standards; flora, fauna and biodiversity; land and water management; rehabilitation of degraded areas; remnant vegetation and conservation areas; soils; and natural hazards in urban and industrial environments or other environments affected by construction activities are included in Divisions 18 Environmental management and 19 Environmental policy, climate change and natural hazards.',4),
('129901','Adaptation to climate change in construction','','',4),
('129999','Other construction not elsewhere classified','','',4),
('13','CULTURE AND SOCIETY','This division includes R&D directed towards improved understanding of culture.','a) Communications equipment is included in Group 2404 Computer, electronic and communication equipment.
b) Urban and regional planning unrelated to heritage are included in Group 1204 Construction planning.
c) Information and communication services, including media, library and information services are included in Division 22 Information and communication services.
d) Recreational services mainly of a commercial nature (e.g. casino, cinema) are included in Group 1199 Other commercial services and tourism.
e) Moral development in individuals is included in Group 1601 Learner and learning.
f) Understanding current social, political and economic aspects of other countries is included in Group 2303 International relations.',4),
('1301','Arts','This group covers R&D directed towards the arts.','a) Film and video services are included in Group 2205 Media services.
b) Cinemas or motion picture exhibition is included in Group 1199 Other commercial services and tourism.',4),
('130101','Design','','',4),
('130102','Music','','',4),
('130103','The creative arts','','',4),
('130104','The performing arts','','',4),
('130199','Arts not elsewhere classified','','',4),
('1302','Communication','This group covers R&D directed towards improving understanding of the social and cultural contexts of communication.','a) Communications equipment is included in Group 2201 Communication technologies, systems and services. 
b) Information and communication services, including media, library and information services are included in Division 22 Information and communication services.',4),
('130201','Communication across languages and culture','','',4),
('130202','Languages and linguistics','','',4),
('130203','Literature','','',4),
('130204','The media','','',4),
('130205','Visual communication','','',4),
('130299','Communication not elsewhere classified','','',4),
('1303','Ethics','This group covers R&D directed towards the understanding and analysis of ethics.','a) Moral development in individuals is included in Group 1601 Learner and learning.
b) Religion and related practices are included in Group 1305 Religion.',4),
('130301','Bioethics','','',4),
('130302','Business ethics','','',4),
('130303','Environmental ethics','','',4),
('130304','Social ethics','','',4),
('130305','Technological ethics','','',4),
('130306','Workplace and organisational ethics (excl. business ethics)','','',4),
('130399','Ethics not elsewhere classified','','',4),
('1304','Heritage','This group covers R&D towards heritage issues, including the conservation of natural, social and cultural heritage, and heritage aspects of urban and regional planning.','a) Indigenous heritage is included in Division 21 Indigenous.',4),
('130401','Assessment of heritage value','','',4),
('130402','Conserving collections and movable cultural heritage','','',4),
('130403','Conserving intangible cultural heritage','','',4),
('130404','Conserving natural heritage','','',4),
('130405','Conserving the historic environment','','',4),
('130499','Heritage not elsewhere classified','','',4),
('1305','Religion','This group covers R&D directed towards the understanding of religion.','a) Ethics is included in Group 1303 Ethics.',4),
('130501','Religion and society','','',4),
('130502','Religious philosophies and belief systems','','',4),
('130503','Religious rituals and traditions (excl. structures)','','',4),
('130504','Religious structures','','',4),
('130599','Religion not elsewhere classified','','',4),
('1306','Sport, exercise and recreation','This group covers R&D directed towards the provision and study of sport, exercise and recreation activities.','a) Tourism services is included in Group 1104 Tourism services.',4),
('130601','Exercise','','',4),
('130602','Organised sports','','',4),
('130603','Recreation and leisure activities (excl. sport and exercise)','','',4),
('130699','Sport, exercise and recreation not elsewhere classified','','',4),
('1307','Understanding past societies','This group covers R&D directed towards the understanding of past societies.','a) Understanding current social, political and economic aspects of other countries is included in Group 2303 International relations.',4),
('130701','Understanding Africa’s past','','',4),
('130702','Understanding Asia’s past','','',4),
('130703','Understanding Australia’s past','','',4),
('130704','Understanding Europe’s past','','',4),
('130705','Understanding New Zealand’s past','','',4),
('130706','Understanding the past of the Americas','','',4),
('130799','Understanding past societies not elsewhere classified','','',4),
('1399','Other culture and society','This group covers R&D directed towards cultural understanding not elsewhere classified.','a) Communications equipment is included in Group 2201 Communication technologies, systems and services. 
b) Urban and regional planning unrelated to heritage are included in Group 1204 Construction planning.
c) Information and communication services, including media, library and information services are included in Division 22 Information and communication services.
d) Recreational services mainly of a commercial nature (e.g. casino, cinema) are included in Group 1199 Other commercial services and tourism.
e) Moral development in individuals is included in Group 1601 Learner and learning.
f) Understanding current social, political and economic aspects of other countries is included in Group 2303 International relations.',4),
('139999','Other culture and society not elsewhere classified','','',4),
('14','DEFENCE','This Division covers R&D directed towards the development of defence or national security, and the development and testing of military or defence related equipment and materials. It includes R&D undertaken for military reasons regardless of their content or whether they have secondary civil applications, and nuclear and space R&D undertaken for military purposes.','',4),
('1401','Defence','','a) Improvement of methods of producing or processing military or defence related equipment and materials are included in the appropriate groups in other divisions, e.g. military transport equipment is included in Group 2415 Transport equipment.
b) Civil oriented projects although funded by defence agencies are included in the appropriate groups in other divisions, e.g. communication is included in Group 2201 Communication technologies, systems and services.
c) Development of measurement standards and calibration for defence related products and processes are included in Group 1504 Measurement standards and calibration services.
d) R&D directed towards improving relationship with other countries for the purpose of improving national and international defence and security is included in Group 2303 International relations.',4),
('140101','Air','','',4),
('140102','Command, control and communications','','',4),
('140103','Cyber and electronic security and warfare','','',4),
('140104','Emerging defence technologies','','',4),
('140105','Intelligence, surveillance and space','','',4),
('140106','Land','','',4),
('140107','Logistics','','',4),
('140108','Maritime','','',4),
('140109','National security','','',4),
('140110','Personnel','','',4),
('140199','Defence not elsewhere classified','','',4),
('15','ECONOMIC FRAMEWORK','This division covers R&D directed towards the understanding of the economic framework of development and the application of economic theory to assist development. It includes R&D directed towards developing measurement standards and calibrations services which make a contribution to the economic framework.','a) Manufacture of scientific, industrial and medical instruments is included in Group 2410 Instrumentation.
b) International transport of freight and passengers is included in Division 27 Transport.
c) Commercial services, including financial and investment services, market research, administration, business support and professional services, domestic trade and economic issues relating to tourism are included in Division 11 Commercial services and tourism.
d) Economics of health policy outcomes is included in Group 2003 Provision of health and support services.
e) Occupational health is included in Group 2005 Specific population health (excl. Indigenous health).
f) International political economy, other than international trade, and international relations that do not relate to trade are included in Group 2303 International relations.
g) Employment, work and workplace safety issues are included in Group 2305 Work and labour market.
h) Environmental aspects of resource consumption and international trade are included in Group 1902 Environmental policy, legislation and standards.',4),
('1501','International trade policy','This group covers R&D directed towards understanding issues relating to international trade, including trade policy, assistance and protection and international agreements on trade.','a) Efficiency, safety and utility of international transport of freight and passengers is included in Division 27 Transport.
b) Domestic trade is included in Group 1103 Property, business support services and trade.
c) International political economy, other than international trade, and international relations that do not relate to trade are included in Group 2303 International relations.
d) Environmental aspects of international trade are included in Group 1902 Environmental policy, legislation and standards.',4),
('150101','International agreements on trade','','',4),
('150102','Trade assistance and protection','','',4),
('150103','Trade policy','','',4),
('150199','International trade policy not elsewhere classified','','',4),
('1502','Macroeconomics','This group covers R&D directed towards the understanding of economic issues at the macroeconomic level, including fiscal policy, monetary policy, exchange rates, balance of payments, income policy, taxation and other macroeconomic issues.','a) Provision of financial and investment services is included in Group 1103 Property, business support services and trade.
b) Economic issues of tourism are included in Group 1104 Tourism services. 
c) Economics of health policy outcomes are included in Group 2003 Provision of health and support services.',4),
('150201','Balance of payments','','',4),
('150202','Demography','','',4),
('150203','Economic growth','','',4),
('150204','Exchange rates','','',4),
('150205','Fiscal policy','','',4),
('150206','Income distribution','','',4),
('150207','Macro labour market issues','','',4),
('150208','Monetary policy','','',4),
('150209','Savings and investments','','',4),
('150210','Taxation','','',4),
('150299','Macroeconomics not elsewhere classified','','',4),
('1503','Management and productivity','This group covers R&D directed towards management and productivity issues. It includes R&D directed towards marketing and industrial relations.','a) The provision of market research, administration, business support and professional services are included in Division 11 Commercial services and tourism.
b) Occupational health is included in Group 2005 Specific population health (excl. Indigenous health).
c) Employment, work and workplace safety issues are included in Group 2305 Work and labour market.',4),
('150301','Industrial relations','','',4),
('150302','Management','','',4),
('150303','Marketing','','',4),
('150304','Productivity (excl. public sector)','','',4),
('150305','Public sector productivity','','',4),
('150306','Technological and organisational innovation','','',4),
('150399','Management and productivity not elsewhere classified','','',4),
('1504','Measurement standards and calibration services','This group covers R&D directed towards the development and provision of measurement standards and calibration services for defence, manufacturing and the service industries.','a) Manufacture of scientific, industrial and medical instruments is included in Group 2410 Instrumentation.',4),
('150401','Agricultural and environmental standards and calibrations','','',4),
('150402','Defence standards and calibrations','','',4),
('150403','Manufacturing standards and calibrations','','',4),
('150404','Service industries standards and calibrations','','',4),
('150499','Measurement standards and calibration services not elsewhere classified','','',4),
('1505','Microeconomics','This group covers R&D directed towards understanding economic issues at the microeconomic level, including effects on taxation, consumption, industrial organisations, industry policy, industry cost and structure and other microeconomic issues.','a) Economics of health policy outcomes is included in Group 2003 Provision of health and support services.
b) Environmental aspects of resource consumption are included in Group 1902 Environmental policy, legislation and standards.',4),
('150501','Consumption','','',4),
('150502','Human capital issues','','',4),
('150503','Industrial organisations','','',4),
('150504','Industry costs and structure','','',4),
('150505','Industry policy','','',4),
('150506','Market-based mechanisms','','',4),
('150507','Micro labour market issues','','',4),
('150508','Microeconomic effects of taxation','','',4),
('150509','Preference, behaviour and welfare','','',4),
('150510','Production','','',4),
('150511','Supply and demand','','',4),
('150599','Microeconomics not elsewhere classified','','',4),
('1599','Other economic framework','This group covers R&D directed towards understanding other economic issues in the economic framework not elsewhere classified. It includes carbon and emissions trading mechanisms and ecological economics.','a) Manufacture of scientific, industrial and medical instruments is included in Group 2410 Instrumentation.
b) International transport of freight and passengers is included in Division 27 Transport.
c) Commercial services, including financial and investment services, market research, administration, business support and professional services, domestic trade and economic issues relating to tourism are included in Division 11 Commercial services and tourism.
d) Economics of health policy outcomes is included in Group 2003 Provision of health and support services.
e) Occupational health is included in Group 2005 Specific population health (excl. Indigenous health).
f) International political economy, other than international trade, and international relations that do not relate to trade are included in Group 2303 International relations.
g) Employment, work and workplace safety issues are included in Group 2305 Work and labour market.
h) Environmental aspects of resource consumption and international trade are included in Group 1902 Environmental policy, legislation and standards.',4),
('159901','Carbon and emissions trading','','',4),
('159902','Ecological economics','','',4),
('159999','Other economic framework not elsewhere classified','','',4),
('16','EDUCATION AND TRAINING','This division covers R&D directed towards improving education and training.','a) Public health education is included in Group 2003 Provision of health and support services.
b) Ethical issues such as business ethics, social ethics, environmental ethics are included in Group 1303 Ethics.
c) Environmental education and awareness is included in Group 1902 Environmental policy, legislation and standards.
d) Education pertaining to Indigenous communities is included in Division 21 Indigenous.',4),
('1601','Learner and learning','This group covers R&D directed towards improving the learning outcomes of individual learners.','a) Improvement of teaching and instruction techniques is included in Group 1603 Teaching and curriculum.
b) Curriculum improvement is included in Group 1603 Teaching and curriculum.
c) Improvement of school and educational institution learning environments is included in Group 1603 Teaching and curriculum.
d) Improvement of education and training systems is included in Group 1603 Teaching and curriculum.
e) Educational needs of Aboriginal and Torres Strait Islanders, Māori and Pacific Peoples are included in Groups 2102 Aboriginal and Torres Strait Islander education, 2106 Māori education and 2110 Pacific Peoples education.
f) Ethical issues such as business ethics, social ethics, environmental ethics are included in Group 1303 Ethics.',4),
('160101','Early childhood education','','',4),
('160102','Higher education','','',4),
('160103','Primary education','','',4),
('160104','Professional development and adult education','','',4),
('160105','Secondary education','','',4),
('160199','Learner and learning not elsewhere classified','','',4),
('1602','Schools and learning environments','This group covers R&D directed towards the improvement of school and educational institution learning environments.','a) Improvement of learning outcomes of individual learners is included in Group 1601 Learner and learning.
b) Improvement of teaching and instruction techniques is included in Group 1603 Teaching and curriculum. 
c) Curriculum improvement is included in Group 1603 Teaching and curriculum. 
d) Improvement of education and training systems is included in Group 1603 Teaching and curriculum. 
e) Development of school or educational institutional environments conducive to the education of Aboriginal and Torres Strait Islanders, Māori and Pacific Peoples are included in Groups 2102 Aboriginal and Torres Strait Islander education, 2106 Māori education and 2110 Pacific Peoples education.',4),
('160201','Equity and access to education','','',4),
('160202','Gender aspects in education','','',4),
('160203','Inclusive education','','',4),
('160204','Management, resources and leadership','','',4),
('160205','Policies and development','','',4),
('160206','Workforce transition and employment','','',4),
('160299','Schools and learning environments not elsewhere classified','','',4),
('1603','Teaching and curriculum','This group covers R&D directed towards improving teaching and instruction.','a) Improvement of learning outcomes of individual learners is included in Group 1601 Learner and learning.
b) Curriculum improvement is included in Group 1603 Teaching and curriculum.
c) Improvement of school and educational institution learning environments is included in Group 1603 Teaching and curriculum.
d) Improvement of education and training systems is included in Group 1603 Teaching and curriculum.
e) Teaching and instruction of Aboriginal and Torres Strait Islanders, Māori and Pacific Peoples are included in Groups 2102 Aboriginal and Torres Strait Islander education, 2106 Māori education and 2110 Pacific Peoples education.',4),
('160301','Assessment, development and evaluation of curriculum','','',4),
('160302','Pedagogy','','',4),
('160303','Teacher and instructor development','','',4),
('160304','Teaching and instruction technologies','','',4),
('160399','Teaching and curriculum not elsewhere classified','','',4),
('1699','Other education and training','This group covers R&D directed towards education and training not elsewhere classified.','a) Public health education is included in Group 2003 Provision of health and support services.
b) Ethical issues such as business ethics, social ethics, environmental ethics are included in Group 1303 Ethics.
c) Environmental education and awareness is included in Group 1902 Environmental policy, legislation and standards.',4),
('169999','Other education and training not elsewhere classified','','',4),
('17','ENERGY','This division covers R&D directed towards energy resources and the supply of energy.','a) The characteristics, propagation, growing and harvesting of crops for use as biomass or conversion to biofuels is included in Division 26 Plant production and plant primary products.
b) Production of non-energy mineral resources is included in Division 25 Mineral resources (excl. energy resources).
c) Manufacture of processed products from energy mineral resources (e.g. lubricants, plastics, synthetic resins and paints) is included in Division 24 Manufacturing.
d) Design of buildings to minimise energy consumption in the finished building is included in Group 1202 Construction design.
e) Distribution of non-energy products, other than water, through pipelines is included in Group 2799 Other transport.
f) Water distribution and waste handling services such as collection, storage, recycling and disposal provided by local councils or private or non-profit waste management businesses are included in Group 1105 Water and waste services.
g) Air quality; control of pests, diseases and exotic species; ecosystem assessment and management; flora, fauna and biodiversity; land and water management; rehabilitation of degraded areas; remnant vegetation and conservation areas; soils; and natural hazards in mining environments or other environments affected by energy activities and greenhouse gas sequestration are included in Divisions 18 Environmental management and 19 Environmental policy, climate change and natural hazards.',4),
('1701','Energy efficiency','This group covers R&D directed towards the efficient use of energy in industry, transport, commerce and households. It includes energy used in forms such as petroleum products, electricity, gas, coke and briquettes and in the form of heat energy.','a) Design of buildings to minimise energy consumption in the finished building is included in Group 1202 Construction design.',4),
('170101','Commercial energy efficiency','','',4),
('170102','Industrial energy efficiency','','',4),
('170103','Residential energy efficiency','','',4),
('170104','Transport energy efficiency','','',4),
('170199','Energy efficiency not elsewhere classified','','',4),
('1702','Energy exploration','This group covers R&D directed towards exploration for energy mineral and geothermal energy resources.','a) Exploration for non-energy mineral resources is included in Group 2503 Mineral exploration.
b) Air quality; control of pests, diseases and exotic species; ecosystem assessment and management; flora, fauna and biodiversity; land and water management; rehabilitation of degraded areas; remnant vegetation and conservation areas; soils; and natural hazards in mining environments are included in Divisions 18 Environmental management and 19 Environmental policy, climate change and natural hazards.',4),
('170201','Coal exploration','','',4),
('170202','Geothermal exploration','','',4),
('170203','Oil and gas exploration','','',4),
('170204','Oil shale and tar sands exploration','','',4),
('170205','Uranium exploration','','',4),
('170299','Energy exploration not elsewhere classified','','',4),
('1703','Energy storage, distribution and supply','This group covers R&D directed towards improving energy distribution including electricity and gas transmission and distribution.','a) Distribution of non-energy products, other than water, through pipelines is included in Group 2799 Other transport.
b) Distribution of water is included in Group 1105 Water and waste services.',4),
('170301','Battery storage','','',4),
('170302','Carbon capture and storage','','',4),
('170303','Energy services and utilities','','',4),
('170304','Energy storage (excl. hydrogen and batteries)','','',4),
('170305','Energy systems and analysis','','',4),
('170306','Energy transmission and distribution (excl. hydrogen)','','',4),
('170307','Hydrogen distribution','','',4),
('170308','Hydrogen storage','','',4),
('170309','Smart grids','','',4),
('170399','Energy storage, distribution and supply not elsewhere classified','','',4),
('1704','Energy transformation','This group covers R&D directed towards the transformation of energy resource minerals into heat, steam or electricity; and the conversion of coal, natural gas, oil shale and tar sands to liquid fuels. It includes hydrogen-based energy systems and fuel cells.','a) Transformation of renewable energy resources into heat, steam or electricity is included in Group 1708 Renewable energy.
b) Engines for use in transport other than internal hydrogen combustion engines are included in Group 2415 Transport equipment.',4),
('170401','Fuel cells (excl. solid oxide)','','',4),
('170402','Hydrogen-based energy systems','','',4),
('170403','Nuclear energy','','',4),
('170404','Solid oxide fuel cells','','',4),
('170405','Transformation of coal into electricity','','',4),
('170406','Transformation of coal into fuels','','',4),
('170407','Transformation of electricity and material feedstocks into fuels','','',4),
('170408','Transformation of gas into electricity','','',4),
('170409','Transformation of gas into fuels','','',4),
('170499','Energy transformation not elsewhere classified','','',4),
('1705','Environmentally sustainable energy activities','This group covers R&D directed towards environmentally sustainable exploration, mining, extraction, preparation and processing of energy mineral resources, and the transformation, storage, distribution and supply of energy by minimising the generation of emissions and wastes and by reducing or optimising the consumption of water and other natural resources.','a) Minimisation of input waste (other than water and energy) is to be categorised according to the objective that benefits from the improvement of input use.
b) Waste handling services such as collection, storage, recycling and disposal provided by local councils or private or non-profit waste management businesses are included in Group 1105 Water and waste services.
c) Air quality; control of pests, diseases and exotic species; ecosystem assessment and management; flora, fauna and biodiversity; land and water management; rehabilitation of degraded areas; remnant vegetation and conservation areas; soils; and natural hazards in mining environments or other environments affected by energy activities and greenhouse gas sequestration are included in Divisions 18 Environmental management and 19 Environmental policy, climate change and natural hazards.',4),
('170501','Management of gaseous waste from energy activities (excl. greenhouse gases)','','',4),
('170502','Management of liquid waste from energy activities (excl. water)','','',4),
('170503','Management of solid waste from energy activities','','',4),
('170504','Management of water consumption by energy activities','','',4),
('170599','Environmentally sustainable energy activities not elsewhere classified','','',4),
('1706','Mining and extraction of energy resources','This group covers R&D directed towards mining and extracting energy mineral resources, as well as the extraction of geothermal energy.','a) Mining and extraction of non-energy mineral resources are included in Group 2504 Primary mining and extraction of minerals.
b) Air quality; control of pests, diseases and exotic species; ecosystem assessment and management; flora, fauna and biodiversity; land and water management; rehabilitation of degraded areas; remnant vegetation and conservation areas; soils; and natural hazards in mining environments are included in Divisions 18 Environmental management and 19 Environmental policy, climate change and natural hazards.',4),
('170601','Coal mining and extraction','','',4),
('170602','Geothermal energy extraction','','',4),
('170603','Oil and gas extraction','','',4),
('170604','Oil shale and tar sands mining and extraction','','',4),
('170605','Uranium mining and extraction','','',4),
('170699','Mining and extraction of energy resources not elsewhere classified','','',4),
('1707','Processing of energy sources','This group covers R&D directed towards the preparation and supply of energy resources, including the production of hydrogen from other energy sources and biofuel production.','a) The characteristics, propagation, growing and harvesting of crops for use as biomass or conversion to biofuels is included in Division 26 Plant production and plant primary products.
b) The processing of non-energy mineral resources is included in Group 2502 First stage treatment of minerals.',4),
('170701','Biomass processing','','',4),
('170702','Hydrogen production from fossil fuels','','',4),
('170703','Hydrogen production from nuclear energy','','',4),
('170704','Hydrogen production from renewable energy','','',4),
('170705','Oil and gas processing','','',4),
('170706','Preparation of black coal','','',4),
('170707','Preparation of brown coal (lignite)','','',4),
('170708','Preparation of oil shale and tar sands','','',4),
('170709','Preparation of uranium','','',4),
('170799','Processing of energy sources not elsewhere classified','','',4),
('1708','Renewable energy','This group covers R&D directed towards the conversion of renewable energy resources into heat, steam or electricity, and improving the generation and supply of renewable energy.','a) Hydrogen production from renewable energy sources is included in Group 1707 Processing of energy sources.
b) Fuel cells and hydrogen-based energy systems are included in Group 1704 Energy transformation.',4),
('170801','Biofuel energy','','',4),
('170802','Geothermal energy','','',4),
('170803','Hydro-electric energy','','',4),
('170804','Solar-photovoltaic energy','','',4),
('170805','Solar-thermal energy','','',4),
('170806','Tidal energy','','',4),
('170807','Wave energy','','',4),
('170808','Wind energy','','',4),
('170899','Renewable energy not elsewhere classified','','',4),
('1799','Other energy','This group covers R&D directed towards energy activities not elsewhere classified.','a) The characteristics, propagation, growing and harvesting of crops for use as biomass or conversion to biofuels is included in Division 26 Plant production and plant primary products.
b) Production of non-energy mineral resources is included in Division 25 Mineral resources (excl. energy resources).
c) Manufacture of processed products from energy mineral resources (e.g. lubricants, plastics, synthetic resins and paints) is included in Division 24 Manufacturing.
d) Design of buildings to minimise energy consumption in the finished building is included in Group 1202 Construction design.
e) Distribution of non-energy products, other than water, through pipelines is included in Group 2799 Other transport.
f) Water distribution and waste handling services such as collection, storage, recycling and disposal provided by local councils or private or non-profit waste management businesses are included in Group 1105 Water and waste services.
g) Air quality; control of pests, diseases and exotic species; ecosystem assessment and management; flora, fauna and biodiversity; land and water management; rehabilitation of degraded areas; remnant vegetation and conservation areas; soils; and natural hazards in mining environments or other environments affected by energy activities and greenhouse gas sequestration are included in Divisions 18 Environmental management and 19 Environmental policy, climate change and natural hazards.',4),
('179999','Other energy not elsewhere classified','','',4),
('18','ENVIRONMENTAL MANAGEMENT','This Division covers R&D directed towards the understanding, management and improvement of the physical environment, both of pristine and degraded or altered conditions.','a) Management of solid, liquid and gaseous wastes generated and water consumption by economic activities is included in their respective Divisions (e.g. construction, manufacturing).
b) Farming and production aspects of animals and primary animal products are included in Division 10 Animal production and animal primary products.
c) Production aspects of agricultural crops, plants and primary plant products are included in Division 26 Plant production and plant primary products.
d) Environmental ethics is included in Group 1303 Ethics.
e) Agricultural and environmental economic measurement standards and calibrations are included in Group 1504 Measurement standards and calibration services.
e) Conservation and efficient use of energy are included in Group 1701 Energy efficiency.
f) Oceanic processes that affect water safety and navigation are included in Group 2704 Water transport.
g) Indigenous connection to environment is included in Division 21 Indigenous.',4),
('1801','Air quality, atmosphere and weather','This group covers R&D directed towards understanding the dynamics of atmospheric and weather systems, and improvement of air quality.','a) Management of greenhouse gas emissions are included in Group 1903 Mitigation of climate change.
b) Climate and climate change are included in Division 19 Environmental policy, climate change and natural hazards.',4),
('180101','Air quality','','',4),
('180102','Atmospheric composition (incl. greenhouse gas inventory)','','',4),
('180103','Atmospheric processes and dynamics','','',4),
('180104','Weather','','',4),
('180199','Air quality, atmosphere and weather not elsewhere classified','','',4),
('1802','Coastal and estuarine systems and management','This group covers R&D directed towards the understanding and management of coastal and estuarine environments. It includes: biodiversity; control of pests, diseases and exotic species; water quality and; rehabilitation or conservation.','a) Rural, regional and urban planning issues, including environmental impact studies of new developments, are included in Group 1204 Construction planning.
b) Economic aspects of resource consumption are included in Group 1505 Microeconomics.
c) Economic aspects of the environment are included in Group 1599 Other economic framework.
d) Control and treatment of human diseases are included in Division 20 Health.
e) Environmental ethics is included in Group 1303 Ethics.
f) Rights to environmental and natural resources are included in Group 1902 Environmental policy, legislation and standards.
g) Domesticated flora and fauna are included in Division 10 Animal production and animal primary products and animal primary products and Division 26 Plant production and plant primary products.',4),
('180201','Assessment and management of coastal and estuarine ecosystems','','',4),
('180202','Coastal erosion','','',4),
('180203','Coastal or estuarine biodiversity','','',4),
('180204','Control of pests, diseases and exotic species in coastal and estuarine environments','','',4),
('180205','Measurement and assessment of estuarine water quality','','',4),
('180206','Rehabilitation or conservation of coastal or estuarine environments','','',4),
('180299','Coastal and estuarine systems and management not elsewhere classified','','',4),
('1803','Fresh, ground and surface water systems and management','This group covers R&D directed towards the understanding and management of fresh, ground and surface water environments. It includes: biodiversity; control of pests, diseases and exotic species; water quality; depletion and; rehabilitation or conservation.','a) Rural, regional and urban planning issues, including environmental impact studies of new developments, are included in Group 1204 Construction planning.
b) Economic aspects of resource consumption are included in Group 1505 Microeconomics.
c) Economic aspects of the environment are included in Group 1599 Other economic framework.
d) Control and treatment of human diseases are included in Division 20 Health.
e) Environmental ethics is included in Group 1303 Ethics.
f) The development of water policy (including water allocation) is included in Group 1902 Environmental policy, legislation and standards.
g) Domesticated flora and fauna are included in Division 10 Animal production and animal primary products and animal primary products and Division 26 Plant production and plant primary products.',4),
('180301','Assessment and management of freshwater ecosystems','','',4),
('180302','Control of pests, diseases and exotic species in fresh, ground and surface water','','',4),
('180303','Fresh, ground and surface water biodiversity','','',4),
('180304','Freshwater assimilative capacity','','',4),
('180305','Ground water quantification, allocation and impact of depletion','','',4),
('180306','Measurement and assessment of freshwater quality (incl. physical and chemical conditions of water)','','',4),
('180307','Rehabilitation or conservation of fresh, ground and surface water environments','','',4),
('180308','Surface water quantification, allocation and impact of depletion','','',4),
('180399','Fresh, ground and surface water systems and management not elsewhere classified','','',4),
('1804','Management of Antarctic and Southern Ocean environments','This group covers R&D directed towards the understanding and management of Antarctic and Southern Ocean environments. It includes: biodiversity; control of pests, diseases and exotic species; water quality and; rehabilitation or conservation.','a) Economic aspects of resource consumption are included in Group 1505 Microeconomics.
b) Economic aspects of the environment are included in Group 1599 Other economic framework.
c) Control and treatment of human diseases are included in Division 20 Health.
d) Environmental ethics is included in Group 1303 Ethics.
e) Rights to environmental and natural resources are included in Group 1902 Environmental policy, legislation and standards.',4),
('180401','Antarctic and Southern Ocean ice dynamics','','',4),
('180402','Antarctic and Southern Ocean oceanic processes','','',4),
('180403','Assessment and management of Antarctic and Southern Ocean ecosystems','','',4),
('180404','Biodiversity in Antarctic and Southern Ocean environments','','',4),
('180405','Control of pests, diseases and exotic species in Antarctic and Southern Ocean environments','','',4),
('180406','Protection and conservation of Antarctic and Southern Ocean environments','','',4),
('180407','Water quality in Antarctic and Southern Ocean environments','','',4),
('180499','Management of Antarctic and Southern Ocean environments not elsewhere classified','','',4),
('1805','Marine systems and management','This group covers R&D directed towards the understanding and management of marine environments. It includes: ecosystems; biodiversity; control of pests, diseases and exotic species; water quality and; rehabilitation or conservation.','a) Economic aspects of resource consumption are included in Group 1505 Microeconomics.
b) Economic aspects of the environment are included in Group 1599 Other economic framework.
c) Control and treatment of human diseases are included in Division 20 Health.
d) Environmental ethics is included in Group 1303 Ethics.
e) Rights to environmental and natural resources are included in Group 1902 Environmental policy, legislation and standards.
f) Domesticated flora and fauna are included in Division 10 Animal production and animal primary products and animal primary products and Division 26 Plant production and plant primary products.',4),
('180501','Assessment and management of benthic marine ecosystems','','',4),
('180502','Assessment and management of pelagic marine ecosystems','','',4),
('180503','Control of pests, diseases and exotic species in marine environments','','',4),
('180504','Marine biodiversity','','',4),
('180505','Measurement and assessment of marine water quality and condition','','',4),
('180506','Oceanic processes (excl. in the Antarctic and Southern Ocean)','','',4),
('180507','Rehabilitation or conservation of marine environments','','',4),
('180599','Marine systems and management not elsewhere classified','','',4),
('1806','Terrestrial systems and management','This group covers R&D directed towards the understanding and management of terrestrial environments. It includes: ecosystems; biodiversity; control of pests, diseases and exotic species; land use; soils and; rehabilitation or conservation.','a) Rural, regional and urban planning issues, including environmental impact studies of new developments, are included in Group 1204 Construction planning.
b) Economic aspects of resource consumption are included in Group 1505 Microeconomics.
c) Economic aspects of the environment are included in Group 1599 Other economic framework.
d) Control and treatment of human diseases are included in Division 20 Health.
e) Environmental ethics is included in Group 1303 Ethics.
f) The development of land policy is included in Group 1902 Environmental policy, legislation and standards.
g) Rights to environmental and natural resources are included in Group 1902 Environmental policy, legislation and standards.
h) Domesticated flora and fauna are included in Division 10 Animal production and animal primary products and animal primary products and Division 26 Plant production and plant primary products.',4),
('180601','Assessment and management of terrestrial ecosystems','','',4),
('180602','Control of pests, diseases and exotic species in terrestrial environments','','',4),
('180603','Evaluation, allocation, and impacts of land use','','',4),
('180604','Rehabilitation or conservation of terrestrial environments','','',4),
('180605','Soils','','',4),
('180606','Terrestrial biodiversity','','',4),
('180607','Terrestrial erosion','','',4),
('180699','Terrestrial systems and management not elsewhere classified','','',4),
('1899','Other environmental management','This group covers R&D directed towards environmental management issues not elsewhere classified.','',4),
('189999','Other environmental management not elsewhere classified','','',4),
('19','ENVIRONMENTAL POLICY, CLIMATE CHANGE AND NATURAL HAZARDS','This Division covers R&D directed towards the development of social and economic environmental policies and understanding the impacts of climate change. It also includes studies of natural hazards, their environmental impact and how to manage this impact.','a) Rural, regional and urban planning issues, including environmental impact studies of new developments, are included in Group 1204 Construction planning.
b) Economic aspects of resource consumption are included in Group 1505 Microeconomics.
c) Economic aspects of the environment are included in Group 1599 Other economic framework.
d) Control and treatment of human diseases within the country''s borders are included in Division 20 Health.
e) Environmental ethics is included in Group 1303 Ethics.',4),
('1901','Adaptation to climate change','This group covers R&D directed towards understanding impacts of climate change and the development of strategies to adapt to climate change.','a) Adaptation to climate change relating to animals is included in Group 1099 Other animal production. 
b) Adaptation to climate change relating to construction is included in Group 1299 Other construction.
c) Climate adaptive plants are included in Group 2699 Other plant production and plant primary products.
d) Strategies to mitigate climate change are included in Group 1903 Mitigation of climate change.
e) Understanding climate change and its effects are included in Group 1905 Understanding climate change.',4),
('190101','Climate change adaptation measures (excl. ecosystem)','','',4),
('190102','Ecosystem adaptation to climate change','','',4),
('190103','Social impacts of climate change and variability','','',4),
('190199','Adaptation to climate change not elsewhere classified','','',4),
('1902','Environmental policy, legislation and standards','This group covers R&D directed towards the development and improvement of social and economic environmental policies, legislation and standards.','a) Rural, regional and urban planning issues, including environmental impact studies of new developments, are included in Group 1204 Construction planning.
b) Economic aspects of resource consumption are included in Group 1505 Microeconomics.
c) Economic aspects of the environment are included in Group 1599 Other economic framework.
d) Control and treatment of human diseases within the country''s borders are included in Division 20 Health.
e) Environmental ethics is included in Group 1303 Ethics.
f) Non-environmental international trade policy issues are included in Group 1501 International trade policy.
g) Indigenous connection to land and environment is included in Division 21 Indigenous.',4),
('190201','Consumption patterns, population issues and the environment','','',4),
('190202','Eco-verification (excl. environmental lifecycle assessment)','','',4),
('190203','Environmental education and awareness','','',4),
('190204','Environmental lifecycle assessment','','',4),
('190205','Environmental protection frameworks (incl. economic incentives)','','',4),
('190206','Institutional arrangements','','',4),
('190207','Land policy','','',4),
('190208','Rights to environmental and natural resources (excl. water allocation)','','',4),
('190209','Sustainability indicators','','',4),
('190210','Trade and environment','','',4),
('190211','Water policy (incl. water allocation)','','',4),
('190299','Environmental policy, legislation and standards not elsewhere classified','','',4),
('1903','Mitigation of climate change','This group covers R&D directed towards the development of strategies to mitigate climate change.','a) Strategies relating to adaptation to climate change are included in Group 1901 Adaptation to climate change.
b) Understanding climate change and its effects are included in Group 1905 Understanding climate change.',4),
('190301','Climate change mitigation strategies','','',4),
('190302','Management of greenhouse gas emissions from animal production','','',4),
('190303','Management of greenhouse gas emissions from commercial services and tourism','','',4),
('190304','Management of greenhouse gas emissions from construction activities','','',4),
('190305','Management of greenhouse gas emissions from electricity generation','','',4),
('190306','Management of greenhouse gas emissions from energy activities','','',4),
('190307','Management of greenhouse gas emissions from information and communication services','','',4),
('190308','Management of greenhouse gas emissions from manufacturing activities','','',4),
('190309','Management of greenhouse gas emissions from mineral resources activities','','',4),
('190310','Management of greenhouse gas emissions from plant production','','',4),
('190311','Management of greenhouse gas emissions from transport activities','','',4),
('190399','Mitigation of climate change not elsewhere classified','','',4),
('1904','Natural hazards','This group covers R&D directed towards an understanding of natural hazards, their impact on lives, property and environments and the development of warning systems and control measures to minimise impacts from these hazards.','',4),
('190401','Climatological hazards (e.g. extreme temperatures, drought and wildfires)','','',4),
('190402','Extraterrestrial hazards (e.g. meteorites)','','',4),
('190403','Geological hazards (e.g. earthquakes, landslides and volcanic activity)','','',4),
('190404','Hydrological hazards (e.g. avalanches and floods)','','',4),
('190405','Meteorological hazards (e.g. cyclones and storms)','','',4),
('190499','Natural hazards not elsewhere classified','','',4),
('1905','Understanding climate change','This group covers R&D directed towards understanding climate change and its effects in various locations.','a) Atmosphere and weather, including atmospheric composition are included in Group 1801 Air quality, atmosphere and weather.
b) Strategies relating to adaptation to climate change are included in Group 1901 Adaptation to climate change.
c) Strategies to mitigate climate change are included in Group 1903 Mitigation of climate change.
d) Environmental ethics is included in Group 1303 Ethics.',4),
('190501','Climate change models','','',4),
('190502','Climate variability (excl. social impacts)','','',4),
('190503','Effects of climate change on Antarctic and sub-Antarctic environments (excl. social impacts)','','',4),
('190504','Effects of climate change on Australia (excl. social impacts)','','',4),
('190505','Effects of climate change on New Zealand (excl. social impacts)','','',4),
('190506','Effects of climate change on the South Pacific (excl. Australia and New Zealand) (excl. social impacts)','','',4),
('190507','Global effects of climate change (excl. Australia, New Zealand, Antarctica and the South Pacific) (excl. social impacts)','','',4),
('190508','Understanding the impact of natural hazards caused by climate change','','',4),
('190599','Understanding climate change not elsewhere classified','','',4),
('1999','Other environmental policy, climate change and natural hazards','This group covers R&D directed towards environmental policy, climate change and natural hazards not elsewhere classified.','',4),
('199999','Other environmental policy, climate change and natural hazards not elsewhere classified','','',4),
('20','HEALTH','This division covers R&D directed towards human health, including the understanding and treatment of clinical diseases and conditions and the provision of public health and associated support services.','a) Development of pharmaceutical products to treat diseases and abnormal conditions is included in Group 2408 Human pharmaceutical products.
b) Community services not directly impacting on health are included in Group 2301 Community services.
c) Workplace safety is included in Group 2305 Work and labour market.
d) Quarantine and control measures to prevent the entry of diseases into the country are included in Division 18 Environmental management.
e) Health pertaining to Indigenous communities is included in Division 21 Indigenous.',4),
('2001','Clinical health','This group covers R&D directed towards the clinical aspects of health and medicine.','a) Development of pharmaceutical products to treat diseases and abnormal conditions is included in Group 2408 Human pharmaceutical products.
b) Preventive medicine and life-style impacts on health, the effect of social conditions (e.g. poverty) on the health of the general community, and the social aspects of health (e.g. food safety, nutrition) are included in Group 2004 Public health (excl. specific population health).
c) Preventive medicine and life-style impacts on health, the effect of social conditions (e.g. poverty) on the health of specific communities, and the social and cultural aspects of health are included in Groups 2103 Aboriginal and Torres Strait Islander health, 2107 Māori health, 2111 Pacific Peoples health and 2005 Specific population health (excl. Indigenous health), respectively.',4),
('200101','Diagnosis of human diseases and conditions','','',4),
('200102','Efficacy of medications','','',4),
('200103','Human pain management','','',4),
('200104','Prevention of human diseases and conditions','','',4),
('200105','Treatment of human diseases and conditions','','',4),
('200199','Clinical health not elsewhere classified','','',4),
('2002','Evaluation of health and support services','This group covers R&D directed towards health not elsewhere classified.','a) Development of pharmaceutical products to treat diseases and abnormal conditions is included in Group 2408 Human pharmaceutical products.
b) Community services not directly impacting on health are included in Group 2301 Community services.
c) Workplace safety is included in Group 2305 Work and labour market.
d) Quarantine and control measures to prevent the entry of diseases into the country are included in Division 18 Environmental management.',4),
('200201','Determinants of health','','',4),
('200202','Evaluation of health outcomes','','',4),
('200203','Health education and promotion','','',4),
('200204','Health inequalities','','',4),
('200205','Health policy evaluation','','',4),
('200206','Health system performance (incl. effectiveness of programs)','','',4),
('200207','Social structure and health','','',4),
('200208','Telehealth','','',4),
('200299','Evaluation of health and support services not elsewhere classified','','',4),
('2003','Provision of health and support services','This group covers R&D directed towards the provision and evaluation of health and support services.','a) Manufacture of scientific, industrial and medical instruments is included in Group 2410 Instrumentation.
b) International transport of freight and passengers is included in Division 27 Transport.
c) Commercial services, including financial and investment services, market research, administration, business support and professional services, domestic trade and economic issues relating to tourism are included in Division 11 Commercial services and tourism.
d) Economics of health policy outcomes is included in Group 2003 Provision of health and support services.
e) Occupational health is included in Group 2005 Specific population health (excl. Indigenous health).
f) International political economy, other than international trade, and international relations that do not relate to trade are included in Group 2303 International relations.
g) Employment, work and workplace safety issues are included in Group 2305 Work and labour market.
h) Environmental aspects of resource consumption and international trade are included in Group 1902 Environmental policy, legislation and standards.',4),
('200301','Allied health therapies (excl. mental health services)','','',4),
('200302','Community health care','','',4),
('200303','Health surveillance','','',4),
('200304','Inpatient hospital care','','',4),
('200305','Mental health services','','',4),
('200306','Midwifery','','',4),
('200307','Nursing','','',4),
('200308','Outpatient care','','',4),
('200309','Palliative care','','',4),
('200310','Primary care','','',4),
('200311','Urgent and critical care, and emergency medicine','','',4),
('200399','Provision of health and support services not elsewhere classified','','',4),
('2004','Public health (excl. specific population health)','This group covers R&D directed towards community health or the health of general public excluding specific population groups and Indigenous communities. The primary focus of R&D in this group is on the social and community aspects of health and medicine.','a) The causes, effects and treatment of specific clinical conditions are included in Group 2001 Clinical health.
b) Evaluation of health care programs and policies is included in Group 2003 Provision of health and support services.
c) Public health of indigenous communities and specific population groups is included in Groups 2103 Aboriginal and Torres Strait Islander health, 2107 Māori health, 2111 Pacific Peoples health and 2005 Specific population health (excl. Indigenous health), respectively.
d) Quarantine and control measures to prevent the entry of diseases into the country are included in Division 18 Environmental management.',4),
('200401','Behaviour and health','','',4),
('200402','Dental health','','',4),
('200403','Disability and functional capacity','','',4),
('200404','Disease distribution and transmission (incl. surveillance and response)','','',4),
('200405','Food safety','','',4),
('200406','Health protection and disaster response','','',4),
('200407','Health status (incl. wellbeing)','','',4),
('200408','Injury prevention and control','','',4),
('200409','Mental health','','',4),
('200410','Nutrition','','',4),
('200411','Overweight and obesity','','',4),
('200412','Preventive medicine','','',4),
('200413','Substance abuse','','',4),
('200499','Public health (excl. specific population health) not elsewhere classified','','',4),
('2005','Specific population health (excl. Indigenous health)','This group covers R&D directed towards community and public health of specific population groups and discrete community groups (e.g. aged persons, persons living in rural and remote areas), other than indigenous communities. It includes the study of social and cultural factors affecting the health of these groups and the delivery of health services to these groups.','a) The causes, effects and treatment of specific clinical conditions is included in Group 2001 Clinical health.
b) Evaluation of health care programs and policies is included in Group 2003 Provision of health and support services.
c) Public health of Indigenous communities and the general population is included in Groups 2103 Aboriginal and Torres Strait Islander health, 2107 Māori health, 2111 Pacific Peoples health and 2004 Public health (excl. specific population health), respectively.
d) Workplace safety is included in Group 2305 Work and labour market.',4),
('200501','Adolescent health','','',4),
('200502','Health related to ageing','','',4),
('200503','Health related to specific ethnic groups','','',4),
('200504','Men''s health','','',4),
('200505','Migrant health','','',4),
('200506','Neonatal and child health','','',4),
('200507','Occupational health','','',4),
('200508','Rural and remote area health','','',4),
('200509','Women''s and maternal health','','',4),
('200599','Specific population health (excl. Indigenous health) not elsewhere classified','','',4),
('2099','Other health','This group covers R&D directed towards health not elsewhere classified.','a) Development of pharmaceutical products to treat diseases and abnormal conditions is included in Group 2408 Human pharmaceutical products.
b) Community services not directly impacting on health are included in Group 2301 Community services.
c) Workplace safety is included in Group 2305 Work and labour market.
d) Quarantine and control measures to prevent the entry of diseases into the country are included in Division 18 Environmental management.',4),
('209999','Other health not elsewhere classified','','',4),
('21','INDIGENOUS','This division covers R&D directed towards understanding Aboriginal and Torres Strait Islander, Māori, Pacific and other Indigenous peoples, nations, communities, languages, places, cultures or knowledges.','a) Understanding of non-Indigenous cultures is included in Division 13 Culture and society.
b) Improving education and training for non-Indigenous populations is included in Division 16 Education and training.
c) Health for non-Indigenous populations is included in Division 20 Health.',4),
('2101','Aboriginal and Torres Strait Islander community services','','',4),
('210101','Aboriginal and Torres Strait Islander community service programs','','',4),
('210102','Aboriginal and Torres Strait Islander development and wellbeing','','',4),
('210199','Aboriginal and Torres Strait Islander community services not elsewhere classified','','',4),
('2102','Aboriginal and Torres Strait Islander education','','',4),
('210201','Aboriginal and Torres Strait Islander education engagement and attendance outcomes','','',4),
('210202','Aboriginal and Torres Strait Islander education system performance','','',4),
('210203','Aboriginal and Torres Strait Islander literacy and numeracy outcomes','','',4),
('210299','Aboriginal and Torres Strait Islander education not elsewhere classified','','',4),
('2103','Aboriginal and Torres Strait Islander health','','',4),
('210301','Aboriginal and Torres Strait Islander determinants of health','','',4),
('210302','Aboriginal and Torres Strait Islander health status and outcomes','','',4),
('210303','Aboriginal and Torres Strait Islander health system performance','','',4),
('210399','Aboriginal and Torres Strait Islander health not elsewhere classified','','',4),
('2104','Aboriginal and Torres Strait Islander heritage and culture','','',4),
('210401','Aboriginal and Torres Strait Islander artefacts','','',4),
('210402','Aboriginal and Torres Strait Islander connection to land and environment','','',4),
('210403','Aboriginal and Torres Strait Islander customary practices','','',4),
('210404','Aboriginal and Torres Strait Islander knowledge','','',4),
('210405','Aboriginal and Torres Strait Islander places of significance','','',4),
('210406','Aboriginal and Torres Strait Islander tradition','','',4),
('210407','Conserving Aboriginal and Torres Strait Islander heritage and culture','','',4),
('210499','Aboriginal and Torres Strait Islander heritage and culture not elsewhere classified','','',4),
('2105','Ngā ratonga hapori Māori (Māori community services)','','',4),
('210501','Ngā hōtaka ratonga hapori Māori (Māori community service programs)','','',4),
('210502','Te whanaketanga me te oranga o te Māori (Māori development and wellbeing)','','',4),
('210599','Ngā ratonga hapori o te Māori kāore anō kia whakarōpūtia i wāhi kē (Māori community services not elsewhere classified)','','',4),
('2106','Mātauranga Māori (Māori education)','','',4),
('210601','Te whai wāhi o te Māori ki te mātauranga me te taetae atu ki te kura (Māori education engagement and attendance outcomes)','','',4),
('210602','Te whakatutukitanga o te pūnaha mātauranga Māori (Māori education system performance)','','',4),
('210603','Ngā putanga reo matatini me te pāngarau o te Māori (Māori literacy and numeracy outcomes)','','',4),
('210699','Te mātauranga Māori kāore kāore anō kia whakarōpūtia i wāhi kē (Māori education not elsewhere classified)','','',4),
('2107','Te hauora (hauora Māori) (Māori health)','','',4),
('210701','Ngā tokoingoa hauora (determinants of Māori health)','','',4),
('210702','Te tūnga me ngā putanga hauora (Māori health status and outcomes)','','',4),
('210703','Te whakatutukinga o te pūnaha hauora (Māori health system performance)','','',4),
('210799','Te hauora kāore anō kia whakarōpūtia i wāhi kē (Māori health not elsewhere classified)','','',4),
('2108','Te tuku ihotanga me te ahurea Māori (Māori heritage and culture)','','',4),
('210801','Āhuatanga Māori (te tuku ihotanga Māori) (Māori tradition)','','',4),
('210802','Te whāomoomo i te tuku ihotanga me te ahurea Māori (conserving Māori heritage and culture)','','',4),
('210803','Mōhiotanga Māori (Māori knowledge)','','',4),
('210804','Ngā taonga (Māori artefacts)','','',4),
('210805','Ngā tikanga Māori (Māori customary practices)','','',4),
('210806','Ngā wāhi taonga (Māori places of significance)','','',4),
('210899','Te tuku ihotanga me te ahurea Māori kāore anō kia whakarōpūhia i wāhi kē (Māori heritage and culture not elsewhere classified)','','',4),
('2109','Pacific Peoples community services','','',4),
('210901','Pacific Peoples community service programs','','',4),
('210902','Pacific Peoples development and wellbeing','','',4),
('210999','Pacific Peoples community services not elsewhere classified','','',4),
('2110','Pacific Peoples education','','',4),
('211001','Pacific Peoples education engagement and attendance','','',4),
('211002','Pacific Peoples education system performance','','',4),
('211003','Pacific Peoples literacy and numeracy outcomes','','',4),
('211099','Pacific Peoples education not elsewhere classified','','',4),
('2111','Pacific Peoples health','','',4),
('211101','Pacific Peoples determinants of health','','',4),
('211102','Pacific Peoples health status and outcomes','','',4),
('211103','Pacific Peoples health system performance','','',4),
('211199','Pacific Peoples health not elsewhere classified','','',4),
('2112','Pacific Peoples heritage and culture','','',4),
('211201','Conserving Pacific Peoples heritage and culture','','',4),
('211202','Pacific Peoples connection to land and environment','','',4),
('211299','Pacific Peoples heritage and culture not elsewhere classified','','',4),
('2199','Other Indigenous','','',4),
('219999','Other Indigenous not elsewhere classified','','',4),
('22','INFORMATION AND COMMUNICATION SERVICES','This division covers R&D directed towards the development, support and provision of information and communication services.','a) Management of energy consumption from information and communication services is included in Group 1701 Energy efficiency.
b) Development and manufacture of computer hardware, communication equipment and electronic equipment, including assembly of components, are included in Division 24 Manufacturing.
c) Installation of communications networks in buildings is included in Group 1201 Building management and services.
d) Waste handling services such as collection, storage, recycling and disposal provided by local councils or private or non-profit waste management businesses are included in Group 1105 Water and waste services.
e) Cinemas or motion picture exhibition is included in Group 1199 Other commercial services and tourism.
f) Music and theatre production mainly of a non-commercial nature is included in Division 13 Culture and society.
g) Social impacts of media services are included in Group 1302 Communication.',4),
('2201','Communication technologies, systems and services','','',4),
('220101','E-infrastructures','','',4),
('220102','Internet protocols (ip)','','',4),
('220103','Mobile technologies and communications','','',4),
('220104','Network security','','',4),
('220105','Network systems and services','','',4),
('220106','Satellite technologies, networks and services','','',4),
('220107','Wireless technologies, networks and services','','',4),
('220199','Communication technologies, systems and services not elsewhere classified','','',4),
('2202','Environmentally sustainable information and communication services','','',4),
('220201','Management of solid waste from information and communication services','','',4),
('220202','Management of water consumption by information and communication services','','',4),
('220299','Environmentally sustainable information and communication services not elsewhere classified','','',4),
('2203','Information services','','',4),
('220301','Digital humanities','','',4),
('220302','Electronic information storage and retrieval services','','',4),
('220303','Library and archival services','','',4),
('220304','Museum and gallery collections','','',4),
('220305','News collection services','','',4),
('220306','Open access','','',4),
('220399','Information services not elsewhere classified','','',4),
('2204','Information systems, technologies and services','','',4),
('220401','Application software packages','','',4),
('220402','Applied computing','','',4),
('220403','Artificial intelligence','','',4),
('220404','Computer systems','','',4),
('220405','Cybersecurity','','',4),
('220406','Graphics','','',4),
('220407','Human-computer interaction','','',4),
('220408','Information systems','','',4),
('220499','Information systems, technologies and services not elsewhere classified','','',4),
('2205','Media services','','',4),
('220501','Animation, video games and computer generated imagery services','','',4),
('220502','Internet, digital and social media','','',4),
('220503','Publishing and print services','','',4),
('220504','Radio, television, film and video services','','',4),
('220599','Media services not elsewhere classified','','',4),
('2299','Other information and communication services','','',4),
('229999','Other information and communication services not elsewhere classified','','',4),
('23','LAW, POLITICS AND COMMUNITY SERVICES','This division covers R&D directed towards the understanding and analysis of law and politics, and towards improving and delivering community services.','a) Defence and national security operations are included in Division 14 Defence.
b) International trade is included in Group 1501 International trade policy.
c) Management and productivity, including industrial relations, are included in Group 1503 Management and productivity.
d) Development and provision of community health services and their associated support services is included in the appropriate groups in Division 20 Health.
e) Ethics, including social, workplace and occupational ethics is included in Group 1303 Ethics.
f) Understanding past societies is included in Group 1307 Understanding past societies.
g) International environmental protection issues are included in Group 1902 Environmental policy, legislation and standards.',4),
('2301','Community services','','',4),
('230101','Ability and disability','','',4),
('230102','Ageing and older people','','',4),
('230103','Carers'' support','','',4),
('230104','Children''s services and childcare','','',4),
('230105','Citizenship and national identity','','',4),
('230106','Employment services','','',4),
('230107','Families and family services','','',4),
('230108','Gender and sexualities','','',4),
('230109','Homelessness and housing services','','',4),
('230110','Migrant and refugee settlement services','','',4),
('230111','Multicultural services','','',4),
('230112','Social class and inequalities','','',4),
('230113','Structure, delivery and resourcing','','',4),
('230114','Violence and abuse services','','',4),
('230115','Youth services','','',4),
('230199','Community services not elsewhere classified','','',4),
('2302','Government and politics','','',4),
('230201','Civics and citizenship','','',4),
('230202','Electoral systems','','',4),
('230203','Political systems','','',4),
('230204','Public services policy advice and analysis','','',4),
('230299','Government and politics not elsewhere classified','','',4),
('2303','International relations','','',4),
('230301','Defence and security policy','','',4),
('230302','International aid and development','','',4),
('230303','International organisations','','',4),
('230304','International political economy (excl. international trade)','','',4),
('230305','Peace and conflict','','',4),
('230399','International relations not elsewhere classified','','',4),
('2304','Justice and the law','','',4),
('230401','Civil justice','','',4),
('230402','Crime prevention','','',4),
('230403','Criminal justice','','',4),
('230404','Law enforcement','','',4),
('230405','Law reform','','',4),
('230406','Legal processes','','',4),
('230407','Legislation, civil and criminal codes','','',4),
('230408','Rehabilitation and correctional services','','',4),
('230499','Justice and the law not elsewhere classified','','',4),
('2305','Work and labour market','','',4),
('230501','Employment patterns and change','','',4),
('230502','Professions and professionalisation','','',4),
('230503','Remuneration','','',4),
('230504','Unpaid work and volunteering','','',4),
('230505','Work and family responsibilities','','',4),
('230506','Workplace safety','','',4),
('230599','Work and labour market not elsewhere classified','','',4),
('2399','Other law, politics and community services','','',4),
('239999','Other law, politics and community services not elsewhere classified','','',4),
('24','MANUFACTURING','','a) Production of unprocessed or semi-processed plant primary products is included in Division 26 Plant production and plant primary products.
b) Production of animals from which raw materials for manufactured products are obtained are included in Division 10 Animal production and animal primary products and animal primary products.
c) Mining, extraction, and first stage processing of minerals are included in Division 25 Mineral resources (excl. energy resources).
d) Energy conservation and efficiency and the mining, extraction, and processing of energy resources used for the manufacture of products are included in Division 17 Energy.
e) The construction of transport infrastructure, installation of communication equipment and testing of the performance, durability and life cycle of manufactured building materials is included in Division 12 Construction.
f) Transport infrastructure (excluding its construction) is included in Division 27 Transport.
g) Use of electronic and communication equipment in communication networks and services is included in Group 2201 Communication technologies, systems and services.
h) Development of computer software and the provision of programming services are included in Group 2204 Information systems, technologies and services.
i) Printing and publishing of newspapers is included in Group 2205 Media services.
j) Waste handling services such as collection, storage, recycling and disposal provided by local councils or private or non-profit waste management businesses are included in Group 1105 Water and waste services.
k) Provision of measurement standards and calibration services for machinery, appliances and equipment is included in Group 1504 Measurement standards and calibration services. 
l) Use of human pharmaceutical products for treatment of human diseases and disorders and manufacturing related environmental health issues are included in the Division 20 Health.',4),
('2401','Agricultural chemicals','','',4),
('240101','Animal protection chemicals','','',4),
('240102','Chemical fertilisers','','',4),
('240103','Crop and pasture protection chemicals','','',4),
('240199','Agricultural chemicals not elsewhere classified','','',4),
('2402','Basic metal products','','',4),
('240201','Basic aluminium products','','',4),
('240202','Basic copper products','','',4),
('240203','Basic iron and steel products','','',4),
('240204','Basic precious metal products','','',4),
('240205','Basic zinc products','','',4),
('240299','Basic metal products not elsewhere classified','','',4),
('2403','Ceramics, glass and industrial mineral products','','',4),
('240301','Cement products and concrete materials','','',4),
('240302','Ceramics','','',4),
('240303','Clay products','','',4),
('240304','Composite materials','','',4),
('240305','Plaster and plaster products','','',4),
('240306','Structural glass and glass products','','',4),
('240399','Ceramics, glass and industrial mineral products not elsewhere classified','','',4),
('2404','Computer, electronic and communication equipment','','',4),
('240401','Computer and electronic office equipment (excl. communication equipment)','','',4),
('240402','Consumer electronic equipment (excl. communication equipment)','','',4),
('240403','Integrated circuits and devices','','',4),
('240404','Integrated systems','','',4),
('240405','Network infrastructure equipment','','',4),
('240406','Processor modules','','',4),
('240407','Robotics','','',4),
('240408','Satellite navigation equipment','','',4),
('240409','Telemetry equipment','','',4),
('240410','Voice and data equipment','','',4),
('240499','Computer, electronic and communication equipment not elsewhere classified','','',4),
('2405','Dairy products','','',4),
('240501','Butter and milk-derived fats and oils (excl. cream)','','',4),
('240502','Casein','','',4),
('240503','Cheese','','',4),
('240504','Processed milk and cream (incl. powder, evaporated and condensed)','','',4),
('240505','Whey','','',4),
('240599','Dairy products not elsewhere classified','','',4),
('2406','Environmentally sustainable manufacturing activities','','',4),
('240601','Development of recyclable or biodegradable componentry, packaging or materials','','',4),
('240602','Management of gaseous waste from manufacturing activities (excl. greenhouse gases)','','',4),
('240603','Management of liquid waste from manufacturing activities (excl. water)','','',4),
('240604','Management of solid waste from manufacturing activities','','',4),
('240605','Management of water consumption by manufacturing activities','','',4),
('240699','Environmentally sustainable manufacturing activities not elsewhere classified','','',4),
('2407','Fabricated metal products','','',4),
('240701','Coated metal and metal-coated products','','',4),
('240702','Machined metal products','','',4),
('240703','Metal castings','','',4),
('240704','Semi-finished metal products','','',4),
('240705','Sheet metal products','','',4),
('240706','Structural metal products','','',4),
('240799','Fabricated metal products not elsewhere classified','','',4),
('2408','Human pharmaceutical products','','',4),
('240801','Human biological preventatives','','',4),
('240802','Human diagnostics','','',4),
('240803','Human pharmaceutical treatments','','',4),
('240899','Human pharmaceutical products not elsewhere classified','','',4),
('2409','Industrial chemicals and related products','','',4),
('240901','Antimicrobials, antifungals and biocides','','',4),
('240902','Bioplastics','','',4),
('240903','Cosmetics','','',4),
('240904','Fine chemicals','','',4),
('240905','Industrial gases','','',4),
('240906','Inorganic industrial chemicals','','',4),
('240907','Lubricants','','',4),
('240908','Organic industrial chemicals (excl. resins, rubber and plastics)','','',4),
('240909','Paints','','',4),
('240910','Plastics','','',4),
('240911','Resins','','',4),
('240912','Rubber','','',4),
('240913','Soaps','','',4),
('240999','Industrial chemicals and related products not elsewhere classified','','',4),
('2410','Instrumentation','','',4),
('241001','Industrial instruments','','',4),
('241002','Medical instruments','','',4),
('241003','Scientific instruments','','',4),
('241099','Instrumentation not elsewhere classified','','',4),
('2411','Leather products, fibre processing and textiles','','',4),
('241101','Clothing','','',4),
('241102','Cotton ginning','','',4),
('241103','Natural fibres, yarns and fabrics','','',4),
('241104','Non-fabric textiles (e.g. felt)','','',4),
('241105','Skins, leather and leather products','','',4),
('241106','Synthetic fibres, yarns and fabrics','','',4),
('241107','Wool scouring and top making','','',4),
('241199','Leather products, fibre processing and textiles not elsewhere classified','','',4),
('2412','Machinery and equipment','','',4),
('241201','3D printers and printing','','',4),
('241202','Autonomous and robotic systems','','',4),
('241203','Electrical machinery and equipment (incl. appliances)','','',4),
('241204','Industrial machinery and equipment','','',4),
('241205','Medical machinery and equipment','','',4),
('241299','Machinery and equipment not elsewhere classified','','',4),
('2413','Processed food products and beverages (excl. dairy products)','','',4),
('241301','Alcoholic beverages','','',4),
('241302','Bakery products','','',4),
('241303','Carcass meat (incl. fish and seafood)','','',4),
('241304','Flour mill and cereal food','','',4),
('241305','Insects','','',4),
('241306','Non-alcoholic beverages (excl. fruit juices and non-dairy milk)','','',4),
('241307','Non-dairy milk','','',4),
('241308','Nutraceuticals and functional foods','','',4),
('241309','Oils and fats (incl. margarines)','','',4),
('241310','Processed fish and seafood products','','',4),
('241311','Processed fruit and vegetable products (incl. juices)','','',4),
('241312','Processed meat products','','',4),
('241313','Sugar, sweeteners and confectionery products','','',4),
('241399','Processed food products and beverages (excl. dairy products) not elsewhere classified','','',4),
('2414','Processed non-food agriculture products (excl. wood, paper and fibre)','','',4),
('241401','Essential oils','','',4),
('241402','Organic fertilisers','','',4),
('241403','Plant extracts','','',4),
('241404','Prepared animal feed','','',4),
('241499','Processed non-food agricultural products (excl. wood, paper and fibre) not elsewhere classified','','',4),
('2415','Transport equipment','','',4),
('241501','Aerospace equipment','','',4),
('241502','Automotive equipment','','',4),
('241503','Nautical equipment','','',4),
('241504','Rail equipment','','',4),
('241599','Transport equipment not elsewhere classified','','',4),
('2416','Veterinary pharmaceutical products','','',4),
('241601','Veterinary biological preventatives','','',4),
('241602','Veterinary diagnostics','','',4),
('241603','Veterinary pharmaceutical treatments','','',4),
('241699','Veterinary pharmaceutical products not elsewhere classified','','',4),
('2417','Wood, wood products and paper','','',4),
('241701','Paper products and pulp','','',4),
('241702','Printing and publishing processes','','',4),
('241703','Reconstituted timber products','','',4),
('241704','Wood products','','',4),
('241705','Wood sawing and veneer','','',4),
('241799','Wood, wood products and paper not elsewhere classified','','',4),
('2499','Other manufacturing','','',4),
('249999','Other manufacturing not elsewhere classified','','',4),
('25','MINERAL RESOURCES (EXCL. ENERGY RESOURCES)','This division covers R&D which primarily benefits or has application to the exploration, mining, extraction and processing of mineral resources, other than energy mineral resources. It includes R&D directed towards environmentally sustainable exploration, extraction and processing of mineral resources.','a) Exploration, mining, extraction, and preparation of energy mineral resources and energy efficiency are included in Division 17 Energy.
b) Manufacture of processed products from mineral resources, including refining and smelting, is included in Division 24 Manufacturing.
c) Air quality; control of pests, diseases and exotic species; ecosystem assessment and management; flora, fauna and biodiversity; land and water management; rehabilitation of degraded areas; remnant vegetation and conservation areas; soils; and natural hazards in mining environments are included in Divisions 18 Environmental management and 19 Environmental policy, climate change and natural hazards.',4),
('2501','Environmentally sustainable mineral resource activities','This group covers R&D directed towards environmentally sustainable exploration, mining, extraction, and processing of mineral resources, other than energy mineral resources, by minimising the generation of wastes and by reducing or optimising the consumption of water and other natural resources.','a) Minimisation of input waste (other than water and energy) is to be categorised according to the objective that benefits from the improvement of input use.
b) Energy conservation and efficiency in mining activities is included in Group 1701 Energy efficiency.
c) Waste handling services such as collection, storage, recycling and disposal provided by local councils or private or non-profit waste management businesses are included in Group 1105 Water and waste services.
d) Air quality; control of pests, diseases and exotic species; ecosystem assessment and management; flora, fauna and biodiversity; land and water management; rehabilitation of degraded areas; remnant vegetation and conservation areas; soils; and natural hazards in mining environments are included in Divisions 18 Environmental management and 19 Environmental policy, climate change and natural hazards.',4),
('250101','Management of gaseous waste from mineral resource activities (excl. greenhouse gases)','','',4),
('250102','Management of liquid waste from mineral resource activities (excl. water)','','',4),
('250103','Management of solid waste from mineral resource activities','','',4),
('250104','Management of water consumption by mineral resource activities','','',4),
('250199','Environmentally sustainable mineral resource activities not elsewhere classified','','',4),
('2502','First stage treatment of minerals','','',4),
('250201','Alumina production','','',4),
('250202','Beneficiation of bauxite and aluminium ores (excl. alumina production)','','',4),
('250203','Beneficiation or dressing of iron ores','','',4),
('250204','Beneficiation or dressing of non-metallic minerals (incl. diamonds)','','',4),
('250205','Concentrating processes of base metal ores (excl. aluminium and iron ores)','','',4),
('250206','Production of unrefined precious metal ingots and concentrates','','',4),
('250299','First stage treatment of minerals not elsewhere classified','','',4),
('2503','Mineral exploration','','',4),
('250301','Aluminium ore exploration','','',4),
('250302','Copper ore exploration','','',4),
('250303','Diamond exploration','','',4),
('250304','Iron ore exploration','','',4),
('250305','Precious (noble) metal ore exploration','','',4),
('250306','Stone and clay exploration','','',4),
('250307','Titanium minerals, zircon, and rare earth metal ore (e.g. monazite) exploration','','',4),
('250308','Zinc ore exploration','','',4),
('250399','Mineral exploration not elsewhere classified','','',4),
('2504','Primary mining and extraction of minerals','','',4),
('250401','Mining and extraction of aluminium ores','','',4),
('250402','Mining and extraction of copper ores','','',4),
('250403','Mining and extraction of diamonds','','',4),
('250404','Mining and extraction of iron ores','','',4),
('250405','Mining and extraction of precious (noble) metal ores','','',4),
('250406','Mining and extraction of stone and clay','','',4),
('250407','Mining and extraction of titanium minerals, zircon, and rare earth metal ores (e.g. monazite)','','',4),
('250408','Mining and extraction of zinc ores','','',4),
('250499','Primary mining and extraction of minerals not elsewhere classified','','',4),
('2599','Other mineral resources (excl. energy resources)','','',4),
('259999','Other mineral resources (excl. energy resources) not elsewhere classified','','',4),
('26','PLANT PRODUCTION AND PLANT PRIMARY PRODUCTS','This division covers R&D directed towards improving the characteristics, propagation and growing of crops, plantation forests and the production of associated primary plant products.','a) Growing of pasture, browse and fodder crops, other than cereals grown either partially or entirely for use as fodder or feed grain, is included in Division 10 Animal production and animal primary products.
b) Production of biofuels from plant products is included in Group 1707 Processing of energy sources.
c) Manufacture of processed plant products and their primary products other than biofuels is included in Division 24 Manufacturing.
d) Transportation of plants and plant products other than forest products is included in Division 27 Transport.
e) Operation of irrigation systems and the provision of waste handling services such as collection, storage, recycling and disposal provided by local councils or private or non-profit waste management enterprises are included in Group 1105 Water and waste services.
f) Air quality; control of pests, diseases and exotic species; ecosystem assessment and management; flora, fauna and biodiversity; land and water management; rehabilitation of degraded areas; remnant vegetation and conservation areas; soils; and natural hazards in agricultural environments are included in Divisions 18 Environmental management and 19 Environmental policy, climate change and natural hazards.',4),
('2601','Environmentally sustainable plant production','','',4),
('260101','Management of gaseous waste from plant production (excl. greenhouse gases)','','',4),
('260102','Management of liquid waste from plant production (excl. water)','','',4),
('260103','Management of solid waste from plant production','','',4),
('260104','Management of water consumption by plant production','','',4),
('260199','Environmentally sustainable plant production not elsewhere classified','','',4),
('2602','Forestry','','',4),
('260201','Hardwood plantations','','',4),
('260202','Harvesting and transport of forest products','','',4),
('260203','Integration of farm and forestry','','',4),
('260204','Native forests','','',4),
('260205','Softwood plantations','','',4),
('260299','Forestry not elsewhere classified','','',4),
('2603','Grains and seeds','','',4),
('260301','Barley','','',4),
('260302','Canola','','',4),
('260303','Grain legumes','','',4),
('260304','Linseed','','',4),
('260305','Lupins','','',4),
('260306','Maize','','',4),
('260307','Oats','','',4),
('260308','Rice','','',4),
('260309','Safflower seed','','',4),
('260310','Sorghum','','',4),
('260311','Soybeans','','',4),
('260312','Wheat','','',4),
('260399','Grains and seeds not elsewhere classified','','',4),
('2604','Harvesting and packaging of plant products','','',4),
('260401','Cotton lint and cotton seed','','',4),
('260402','Fresh fruits and vegetables (post harvest)','','',4),
('260403','Sugar cane (cut for crushing)','','',4),
('260404','Unprocessed grains','','',4),
('260405','Unprocessed industrial crops (excl. sugar and cotton)','','',4),
('260406','Unprocessed seeds','','',4),
('260499','Harvesting and packaging of plant products not elsewhere classified','','',4),
('2605','Horticultural crops','','',4),
('260501','Almonds','','',4),
('260502','Avocado','','',4),
('260503','Berry fruit (excl. kiwifruit)','','',4),
('260504','Citrus fruit','','',4),
('260505','Field grown vegetable crops','','',4),
('260506','Kiwifruit','','',4),
('260507','Macadamias','','',4),
('260508','Mushrooms and truffles','','',4),
('260509','Olives','','',4),
('260510','Ornamentals, natives, flowers and nursery plants','','',4),
('260511','Pome fruit, pip fruit','','',4),
('260512','Protected vegetable crops','','',4),
('260513','Stone fruit (excl. avocado)','','',4),
('260514','Table grapes','','',4),
('260515','Tree nuts (excl. almonds and macadamias)','','',4),
('260516','Tropical fruit','','',4),
('260599','Horticultural crops not elsewhere classified','','',4),
('2606','Industrial crops','','',4),
('260601','Cannabis','','',4),
('260602','Cotton','','',4),
('260603','Essential oil crops','','',4),
('260604','Hemp','','',4),
('260605','Hops','','',4),
('260606','Plant extract crops','','',4),
('260607','Sugar','','',4),
('260608','Wine grapes','','',4),
('260699','Industrial crops not elsewhere classified','','',4),
('2699','Other plant production and plant primary products','','',4),
('269901','Climate adaptive plants','','',4),
('269902','Forest product traceability and quality assurance','','',4),
('269903','Plant product traceability and quality assurance (excl. forest products)','','',4),
('269999','Other plant production and plant primary products not elsewhere classified','','',4),
('27','TRANSPORT','This division covers R&D directed towards improving the efficiency, safety and utility of transport systems for moving freight, passengers or livestock by ground, water, air or any combinations of these (e.g. sea–air transport) or by any other means.','a) Movement of defence related personnel, materials and equipment is included in Division 14 Defence.
b) Transportation of forest products is included in Group 2602 Forestry.
c) Pipeline transportation of energy products is included in Group 1703 Energy storage, distribution and supply.
d) Conservation of energy and improving energy efficiency in transport are included in Group 1701 Energy efficiency.
e) Manufacture of transport equipment is included in Group 2415 Transport equipment.
f) Transport systems as an integral part of urban planning and the construction of transport infrastructure are included in Division 12 Construction.
g) Pipeline transportation of water and waste handling services such as collection, storage, recycling and disposal provided by local councils or private or non-profit waste management businesses are included in Group 1105 Water and waste services.
h) Air quality; control of pests, diseases and exotic species; ecosystem assessment and management; flora, fauna and biodiversity; land and water management; rehabilitation of degraded areas; remnant vegetation and conservation areas; soils; and natural hazards in environments affected by transport activities are included in Divisions 18 Environmental management and 19 Environmental policy, climate change and natural hazards.',4),
('2701','Aerospace transport','','',4),
('270101','Air freight','','',4),
('270102','Air passenger transport','','',4),
('270103','Air safety and traffic management','','',4),
('270104','Air terminal infrastructure and management','','',4),
('270105','Autonomous air vehicles','','',4),
('270106','Space transport','','',4),
('270199','Aerospace transport not elsewhere classified','','',4),
('2702','Environmentally sustainable transport activities','','',4),
('270201','Management of gaseous waste from transport activities (excl. greenhouse gases)','','',4),
('270202','Management of liquid waste from transport activities (excl. water)','','',4),
('270203','Management of noise and vibration from transport activities','','',4),
('270204','Management of solid waste from transport activities','','',4),
('270205','Management of water consumption by transport activities','','',4),
('270299','Environmentally sustainable transport activities not elsewhere classified','','',4),
('2703','Ground transport','','',4),
('270301','Active ground transport','','',4),
('270302','Autonomous road vehicles','','',4),
('270303','Heavy rail infrastructure and networks','','',4),
('270304','Rail freight','','',4),
('270305','Rail passenger movements','','',4),
('270306','Rail safety','','',4),
('270307','Road freight','','',4),
('270308','Road infrastructure and networks','','',4),
('270309','Road passenger movements (excl. public transport)','','',4),
('270310','Road public transport','','',4),
('270311','Road safety','','',4),
('270312','Urban rail infrastructure and networks (incl. light and metro rail)','','',4),
('270399','Ground transport not elsewhere classified','','',4),
('2704','Water transport','','',4),
('270401','Autonomous water vehicles','','',4),
('270402','Coastal sea freight transport','','',4),
('270403','Domestic passenger water transport (e.g. ferries)','','',4),
('270404','International passenger water transport (e.g. passenger ships)','','',4),
('270405','International sea freight transport (excl. live animals, food products and liquefied gas)','','',4),
('270406','International sea transport of food products','','',4),
('270407','International sea transport of liquefied gas','','',4),
('270408','International sea transport of live animals','','',4),
('270409','Port infrastructure and management','','',4),
('270410','Water safety','','',4),
('270499','Water transport not elsewhere classified','','',4),
('2799','Other transport','','',4),
('279901','Intermodal materials handling','','',4),
('279902','Multimodal transport','','',4),
('279903','Pipeline transport','','',4),
('279904','Postal and package services (incl. courier services)','','',4),
('279999','Other transport not elsewhere classified','','',4),
('28','EXPANDING KNOWLEDGE','','',4),
('2801','Expanding knowledge','','',4),
('280101','Expanding knowledge in the agricultural, food and veterinary sciences','','',4),
('280102','Expanding knowledge in the biological sciences','','',4),
('280103','Expanding knowledge in the biomedical and clinical sciences','','',4),
('280104','Expanding knowledge in built environment and design','','',4),
('280105','Expanding knowledge in the chemical sciences','','',4),
('280106','Expanding knowledge in commerce, management, tourism and services','','',4),
('280107','Expanding knowledge in the earth sciences','','',4),
('280108','Expanding knowledge in economics','','',4),
('280109','Expanding knowledge in education','','',4),
('280110','Expanding knowledge in engineering','','',4),
('280111','Expanding knowledge in the environmental sciences','','',4),
('280112','Expanding knowledge in the health sciences','','',4),
('280113','Expanding knowledge in history, heritage and archaeology','','',4),
('280114','Expanding knowledge in Indigenous studies','','',4),
('280115','Expanding knowledge in the information and computing sciences','','',4),
('280116','Expanding knowledge in language, communication and culture','','',4),
('280117','Expanding knowledge in law and legal studies','','',4),
('280118','Expanding knowledge in the mathematical sciences','','',4),
('280119','Expanding knowledge in philosophy and religious studies','','',4),
('280120','Expanding knowledge in the physical sciences','','',4),
('280121','Expanding knowledge in psychology','','',4),
('280122','Expanding knowledge in creative arts and writing studies','','',4),
('280123','Expanding knowledge in human society','','',4);