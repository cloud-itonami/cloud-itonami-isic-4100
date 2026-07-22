(ns building.facts
  "Per-jurisdiction building-construction regulatory catalog -- the
  spec-basis table the Building Governor checks every operation against
  ('did the advisor cite an OFFICIAL public source for this
  jurisdiction's building-construction requirements, or did it invent
  one?').

  Coverage is reported HONESTLY: a jurisdiction not in this table has NO
  spec-basis, full stop -- the advisor must not fabricate one, and the
  governor holds if it tries.

  Seed values are drawn from each jurisdiction's official building-code
  and construction-safety authorities (see `:provenance`); this is a
  STARTING catalog (JPN/USA/DEU/GBR), not a from-scratch survey of all
  ~194 jurisdictions. Extending coverage is additive: add one map to
  `catalog`, cite a real source, done -- never invent a jurisdiction's
  requirements to make coverage look bigger.

  Unlike the disaster-safety facts in sibling `cloud-itonami-isic-4211`,
  this actor coordinates site operations (trade dispatch, progress
  logging, safety hazard flagging, inspection scheduling) -- NOT direct
  construction work or building certification. All operations are
  coordination proposals (`:effect :propose`), never final engineering
  sign-off or occupancy certification (those require a licensed inspector
  or engineer's exclusive authority).")

(def catalog
  "iso3 -> requirement map. `:legal-basis` / `:owner-authority` /
  `:provenance` are the G2-style citation the governor requires before
  any operation proposal can commit."
  {"JPN" {:name "Japan"
          :owner-authority "国土交通省 (建築基準法主管官庁) / 厚生労働省 (労働基準局)"
          :legal-basis "建築基準法 / 建設業法 / 労働基準法"
          :provenance "https://laws.e-gov.go.jp/law/322AC0000000201"
          :note "Japanese Building Standards Act (建築基準法) regulates design, construction, and use of buildings; Construction Business Act (建設業法) regulates contractor licensing; Labor Standards Act (労働基準法) regulates worker safety. This actor coordinates site operations under these frameworks but does not certify compliance or issue occupancy permits (exclusive to licensed inspectors/engineers)."}
   "USA" {:name "United States"
          :owner-authority "International Code Council (IBC model code) / International Building Code; Authority Having Jurisdiction (AHJ) at state/local level"
          :legal-basis "International Building Code (IBC) / OSHA Construction Standards (29 CFR 1926)"
          :provenance "https://www.iccsafe.org/products-and-services/standards/2024-ibc/"
          :note "IBC is a model code adopted by most U.S. states and municipalities. Each jurisdiction enforces through their AHJ. OSHA regulates worker safety on construction sites. This actor coordinates site operations (trade dispatch, hazard flagging, inspection scheduling) but does not certify building safety or issue occupancy permits (exclusive to local Building Official/AHJ)."}
   "DEU" {:name "Germany (EU jurisdiction proxy)"
          :owner-authority "Bundesministerium für Wohnen, Stadtentwicklung und Bauwesen (BMWSB); Bauaufsichtsbehörden (state-level building authorities)"
          :legal-basis "Musterbauordnung (MBO, model building code) / Baustellenverordnung (construction sites directive) / Building codes at Bundesland level"
          :provenance "https://www.bauministerkonferenz.de/"
          :note "Building regulation in Germany is administered at the Bundesland (state) level, each with its own Bauordnung based on the Musterbauordnung model. This actor coordinates site operations but does not issue building permits (Baugenehmigung) or occupancy certificates (exclusive to Bauaufsichtsbehörde)."}
   "GBR" {:name "United Kingdom (England building-control framework)"
          :owner-authority "Health and Safety Executive (HSE) -- Building Safety Regulator, for higher-risk buildings in England (Building Safety Act 2022 s.2(1), s.1(4)(a)); local authorities, for other building work (Building Act 1984 s.91, as amended)"
          :legal-basis "Building Safety Act 2022 / Building Act 1984 (as amended) / Building Regulations 2010 (SI 2010/2214, as amended) / Construction (Design and Management) Regulations 2015 (SI 2015/51)"
          :provenance "https://www.legislation.gov.uk/ukpga/2022/30/contents"
          :note "Building Safety Act 2022 s.2(1) defines 'the regulator' (the Building Safety Regulator) as the Health and Safety Executive; per s.1(2)/(4)(a) its building-control-authority role for higher-risk buildings applies to buildings in England. For other building work, Building Act 1984 s.91 (as amended) places the duty to enforce building regulations on local authorities, with the regulator substituting only where later sections designate it the building control authority instead. Building Act 1984 s.1 is the enabling power for building regulations generally (design, construction and demolition of buildings); the current substantive requirements sit in the Building Regulations 2010 (SI 2010/2214) reg.4 (building work must comply with the applicable Schedule 1 requirements). Construction-site worker safety is separately regulated by the Construction (Design and Management) Regulations 2015 (SI 2015/51) reg.8 (general duties of designers/contractors), made under the Health and Safety at Work etc Act 1974 on proposals from HSE. Confirmed coverage here is England-specific: Building Safety Act 2022 s.1(4)(b) notes Wales runs a parallel register of building control approvers and building inspectors administered by the Welsh Ministers, and Scotland/Northern Ireland operate their own separate building-control legislation -- the Welsh, Scottish, and Northern Irish specifics have NOT been independently verified here and are not claimed by this entry (an honest gap, not a fabrication). This actor coordinates site operations under the England framework but does not certify compliance, grant building control approval, or complete a Gateway/registration decision (exclusive to the regulator, a local authority, or a building control approver)."}})

(defn spec-basis
  "The jurisdiction's requirement map, or nil -- nil means NO spec-basis,
  and the governor must hold any proposal that cites it."
  [iso3]
  (get catalog iso3))

(defn coverage
  "Honest coverage report: how many of the requested jurisdictions actually
  have a spec-basis entry. Never report a missing jurisdiction as covered."
  ([] (coverage (keys catalog)))
  ([iso3s]
   (let [have (filter catalog iso3s)
         missing (remove catalog iso3s)]
     {:requested (count iso3s)
      :covered (count have)
      :covered-jurisdictions (vec (sort have))
      :missing-jurisdictions (vec (sort missing))
      :note (str "cloud-itonami-isic-4100 R0: " (count catalog)
                 " jurisdictions seeded with an official spec-basis. "
                 "This is a starting catalog, not a survey of all ~194 "
                 "jurisdictions -- extend `building.facts/catalog`, "
                 "never fabricate a jurisdiction's requirements.")})))
