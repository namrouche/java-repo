parserValeurs = {map,fichier->
		new File(fichier).eachLine(){line->
			line = line.trim()
			if(!line.startsWith("#") && line.contains("=")){
				elts=line.split("=",2)
				if(map.get(elts[0].trim())){fail(elts[0].trim()+" est en double")}
				map.put(elts[0].trim(),['valeur':valeur=elts[1].trim().replace('\\','\\\\'),'used':false])
			}
		}
	}
	
	log.info('---------------------------------------------------------------------------------------')
	log.info('---- Lecture des fichier config                                                      --')
	log.info('---------------------------------------------------------------------------------------')
	
	//fichier de conf applicative
	devAppProperties= project.properties['zone-env-parameters.properties'] ? project.properties['zone-env-parameters.properties'] : project.basedir.absolutePath+"/src/main/resources/env/RLE-Development-parameters.properties"
	log.info('Lecture de la configuration APP de dev à partir de ' + devAppProperties)
	mapVarAppDev = [:]
	parserValeurs(mapVarAppDev,devAppProperties)
	
	//fichier de conf projet
	devProjProperties= project.properties['socle-local-projet.properties'] ? project.properties['socle-local-projet.properties'] : project.basedir.absolutePath+"/src/main/resources/env/socle-local-projet.properties"
	log.info('Lecture de la configuration PROJET de dev à partir de ' + devProjProperties)
	mapVarProjDev = [:]
	parserValeurs(mapVarProjDev,devProjProperties)
	
	//fichier de conf instance
	devInstProperties=project.properties['socle-local-instance.properties'] ? project.properties['socle-local-instance.properties'] : project.basedir.absolutePath+"/src/main/resources/env/socle-local-instance.properties"
	log.info('Lecture de la configuration INSTANCE de dev à partir de ' + devInstProperties)
	mapVarInstDev = [:]
	parserValeurs(mapVarInstDev,devInstProperties)
	
	
	devPath= project.basedir.absolutePath+"/generated-resources/dev"
	ant.mkdir(dir:devPath)
	
	log.info('Suppression de la configuration de dev actuelle')
	dev = new File(devPath)
	ant.delete(includeemptydirs:"true"){
		fileset(dir:dev,includes:"**/*"){}
	}
	
	log.info('Recopie des templates')
	repTemplates=new File(project.properties['templates.dir'] ? project.properties['templates.dir'] : project.basedir.absolutePath+"/src/main/resources/templates")
	ant.copy(todir: dev){
		 fileset(dir: repTemplates) {}
	}
	
	trouverValeur = {variable ->
		if(mapVarAppDev[variable]){
			mapVarAppDev[variable]['used'] = true
			return mapVarAppDev[variable]['valeur']
		} else if(mapVarProjDev[variable]){
			mapVarProjDev[variable]['used'] = true
			return mapVarProjDev[variable]['valeur']
		} else if(mapVarInstDev[variable]){
			mapVarInstDev[variable]['used'] = true
			return mapVarInstDev[variable]['valeur']
		}else if (commonSocleProperties.getProperty(variable)){
			return commonSocleProperties.getProperty(variable)
		} else{
			fail("variable $variable introuvable")
		}
	}
	
	remplaceValeur = {line ->
		log.debug(line)
		if(line.contains("@@")){
			// Echappement des éventuels symboles dollar ($)
			// cf http://cephas.net/blog/2006/02/09/javalangillegalargumentexception-illegal-group-reference-replaceall-and-dollar-signs/
			line = line.replace('$','\\$');
			newText=line.replaceAll("([^@]*)@@([^@]*)@@([^@]*)"){Object[] it ->
				valeur = trouverValeur(it[2]).replace('$','\\$');
				it[1]+valeur+it[3]
			}
			// Déséchappement des éventuels symboles dollar ($)
			newText = newText.replace('\\$','$')
		}
		else{newText=line}
		return newText
	}
	log.info('Création de la configuration de dev')
	
	// TODO project.properties['filter.exlusion'].split(",").contains(file.)
	dev.eachFileRecurse(){ file ->
		if (file ==~ ".*\\.(properties|xml)\$" && !( file ==~ ".*database-populator.*xml\$" )  ){
			newFile = ''
			file.eachLine(){line->
				newLine=line
				while(newLine.contains("@@")){
					newLine=remplaceValeur(newLine)
				}
				newFile += newLine  +'\n'
			}
			file.write(newFile)
		}
	}
	
	log.info("Vérification de l'utilité de tous les paramètres")
		mapVarAppDev.each(){ k,v ->
			if(!v['used']){fail("Dans $devAppProperties la variable $k n'est jamais utilisée.")}
		}
		mapVarProjDev.each(){ k,v ->
			if(!v['used']){fail("Dans $devProjProperties la variable $k n'est jamais utilisée.")}
		}
		mapVarInstDev.each(){ k,v ->
			if(!v['used']){fail("Dans $devInstProperties la variable $k n'est jamais utilisée.")}
		}
