//
//  NativeAddressBook.m
//  ABTest
//
//  Created by Cornelius Ratsch on 23.03.10.
//  Copyright (c) 2010, __MyCompanyName__. All rights reserved.
//

#import <AddressBook/AddressBook.h>
#import <JavaNativeFoundation/JavaNativeFoundation.h> // helper framework for Cocoa and JNI development

#import <time.h>
#import <Foundation/NSDate.h>
#import <CoreServices/CoreServices.h>
#import <CoreFoundation/CoreFoundation.h>


/* 
 * Entry point from Java though JNI to call into the Mac OS X Address Book framework,
 * and create Java objects from the Objective-C Foundation objects returned from the
 * Address Book.
 * 
 * Uses JavaNativeFoundation, a sub-framework of the JavaVM.framework to setup autorelease
 * pools, catch NSExceptions, and re-throw them as Java exceptions. JNF provides a layer
 * on top of conventional C-based JNI to ease interoperability with Cocoa.
 *
 * For more information on conventional JNI on Mac OS X, see:
 * <http://developer.apple.com/technotes/tn2005/tn2147.html>
 */


// coerces an ABPerson into a map
@interface ABPersonCoercer : NSObject<JNFTypeCoercion> { }
+ (ABPersonCoercer *) personCoercer;
@end

// coerces an ABPerson into a map
@interface DateCoercer : NSObject<JNFTypeCoercion> { }
+ (DateCoercer *) dateCoercer;
@end

// coerces an NSNotification into a list of single key/value maps
@interface NSNotificationCoercer : NSObject<JNFTypeCoercion> { }
+ (NSNotificationCoercer *) notificationCoercer;
@end

// coerces an ABMultiValue into a list of single key/value maps
@interface ABMultiValueCoercer : NSObject<JNFTypeCoercion> { }
+ (ABMultiValueCoercer *) multiValueCoercer;
@end

// coerces an ABGroup into a list of single key/value maps
@interface ABGroupCoercer : NSObject<JNFTypeCoercion> { }
+ (ABGroupCoercer *) groupCoercer;
@end

@interface ABNotificationObserver : NSObject { }
- (void) notificationReceived:(NSNotification *)notification;
@end

jclass cls;
jmethodID modifiedMethodID;
JavaVM *javaVM;
bool dontSave = false;

jint GetJNIEnv(JNIEnv **env, bool *mustDetach)
{
	jint getEnvErr = JNI_OK;
	*mustDetach = false;
	if (javaVM) {
		getEnvErr = (*javaVM)->GetEnv(javaVM, (void **)env, JNI_VERSION_1_4);
		if (getEnvErr == JNI_EDETACHED) {
			getEnvErr = (*javaVM)->AttachCurrentThread(javaVM, (void **)env, NULL);
			if (getEnvErr == JNI_OK) {
				*mustDetach = true;
			}
		}
	}
	return getEnvErr;
}

jobject findContactsBySearchElement(ABSearchElement *search, JNIEnv *env) {
	NSArray *peopleFound =
    [[ABAddressBook sharedAddressBook] recordsMatchingSearchElement:search];
	
	JNFTypeCoercer *coercer = [[[JNFTypeCoercer alloc] init] autorelease];
	[JNFDefaultCoercions addStringCoercionTo:coercer];
	[JNFDefaultCoercions addNumberCoercionTo:coercer];
	[JNFDefaultCoercions addListCoercionTo:coercer];
	[JNFDefaultCoercions addMapCoercionTo:coercer];
	[JNFDefaultCoercions addSetCoercionTo:coercer];
	[coercer addCoercion:[DateCoercer dateCoercer] forNSClass:[NSDate class] javaClass:nil];
	[coercer addCoercion:[ABPersonCoercer personCoercer] forNSClass:[ABPerson class] javaClass:nil];
	[coercer addCoercion:[ABMultiValueCoercer multiValueCoercer] forNSClass:[ABMultiValue class] javaClass:nil];
	
	return [coercer coerceNSObject:peopleFound withEnv:env];
}

ABSearchElement * getFirstNameSearchElement(jstring searchString, JNIEnv *env) {
	return [ABPerson searchElementForProperty:kABFirstNameProperty
										label:nil
										  key:nil
										value:JNFJavaToNSString(env, searchString)
								   comparison:kABContainsSubStringCaseInsensitive];
}

ABSearchElement * getMiddleNameSearchElement(jstring searchString, JNIEnv *env) {
	return [ABPerson searchElementForProperty:kABMiddleNameProperty
										label:nil
										  key:nil
										value:JNFJavaToNSString(env, searchString)
								   comparison:kABContainsSubStringCaseInsensitive];
}

ABSearchElement * getLastNameSearchElement(jstring searchString, JNIEnv *env) {
	return [ABPerson searchElementForProperty:kABLastNameProperty
										label:nil
										  key:nil
										value:JNFJavaToNSString(env, searchString)
								   comparison:kABContainsSubStringCaseInsensitive];
}

ABSearchElement * getOrganizationSearchElement(jstring searchString, JNIEnv *env) {
	return [ABPerson searchElementForProperty:kABOrganizationProperty
										label:nil
										  key:nil
										value:JNFJavaToNSString(env, searchString)
								   comparison:kABContainsSubStringCaseInsensitive];
}

ABSearchElement * getPhoneSearchElement(jstring searchString, JNIEnv *env) {
	return [ABPerson searchElementForProperty:kABPhoneProperty
										label:nil
										  key:nil
										value:JNFJavaToNSString(env, searchString)
								   comparison:kABContainsSubStringCaseInsensitive];
}

ABSearchElement * getEmailSearchElement(jstring searchString, JNIEnv *env) {
	return [ABPerson searchElementForProperty:kABEmailProperty
										label:nil
										  key:nil
										value:JNFJavaToNSString(env, searchString)
								   comparison:kABContainsSubStringCaseInsensitive];
}
ABSearchElement * getAddressNameSearchElement(jstring searchString, JNIEnv *env) {
	return [ABPerson searchElementForProperty:kABAddressProperty
										label:nil
										  key:nil
										value:JNFJavaToNSString(env, searchString)
								   comparison:kABContainsSubStringCaseInsensitive];
}

ABSearchElement * getFullTextSearchElement(jstring searchString, JNIEnv *env) {
	return [ABSearchElement searchElementForConjunction:kABSearchOr 
											   children:[NSArray arrayWithObjects:
														 getFirstNameSearchElement(searchString, env),
														 getLastNameSearchElement(searchString, env),
														 getMiddleNameSearchElement(searchString, env),
														 getOrganizationSearchElement(searchString, env),
														 getPhoneSearchElement(searchString, env),
														 getEmailSearchElement(searchString, env),
														 getAddressNameSearchElement(searchString, env),
														 nil]];
}

NSArray * coerceJavaList(jobject obj, JNIEnv * env) {
	JNFTypeCoercer *coercer = [[[JNFTypeCoercer alloc] init] autorelease];
	[JNFDefaultCoercions addStringCoercionTo:coercer];
	[JNFDefaultCoercions addNumberCoercionTo:coercer];
	[JNFDefaultCoercions addListCoercionTo:coercer];
	[JNFDefaultCoercions addMapCoercionTo:coercer];
	
	NSArray *array = (NSArray *)[coercer coerceJavaObject: obj withEnv: env];
	return array;
}

ABMutableMultiValue * coerceJavaMultiValue(jobject obj, JNIEnv * env) {
	
	NSArray *array = coerceJavaList(obj, env);
	ABMutableMultiValue *multi = [[[ABMutableMultiValue alloc] init] autorelease];
	for(NSUInteger i = 0; i < [array count]; ++i) {
		NSDictionary *dict = (NSDictionary *)[array objectAtIndex:i];
		NSArray *keys = [dict allKeys];
		for(NSUInteger j = 0; j < [keys count]; ++j) {
			id value = [dict objectForKey:[keys objectAtIndex:j]];
			[multi addValue:value withLabel:[keys objectAtIndex:j]];
		}
	}
	return multi;
}

jobject coerceRecords(NSArray *records, JNIEnv *env) {
	if(records == nil) {
		return nil;
	}
	// create and load a coercer with all of the different coercions to convert each type of object
	JNFTypeCoercer *coercer = [[[JNFTypeCoercer alloc] init] autorelease];
	[JNFDefaultCoercions addStringCoercionTo:coercer];
	[JNFDefaultCoercions addNumberCoercionTo:coercer];
	[JNFDefaultCoercions addListCoercionTo:coercer];
	[JNFDefaultCoercions addMapCoercionTo:coercer];
	[JNFDefaultCoercions addSetCoercionTo:coercer];
	[coercer addCoercion:[DateCoercer dateCoercer] forNSClass:[NSDate class] javaClass:nil];
	[coercer addCoercion:[ABPersonCoercer personCoercer] forNSClass:[ABPerson class] javaClass:nil];
	[coercer addCoercion:[ABMultiValueCoercer multiValueCoercer] forNSClass:[ABMultiValue class] javaClass:nil];
	[coercer addCoercion:[ABGroupCoercer groupCoercer] forNSClass:[ABGroup class] javaClass:nil];
	
	// recursively decend into the object graph of "people", and 
	// convert every NSObject into a corresponding Java object
	return [coercer coerceNSObject:records withEnv:env];
}

NSArray * getRecordsByUIDs(jobject uids, JNIEnv *env) {
	if(uids == nil) {
		return nil;
	}
	
	NSArray *uidArray = coerceJavaList(uids, env);
	NSMutableArray *nativeRecords = [[[NSMutableArray alloc] init] autorelease];
	
	ABAddressBook *book = [ABAddressBook sharedAddressBook];
	
	for(NSString *uid in uidArray) {
		ABRecord *rec = [book recordForUniqueId:uid];
		if(rec != nil) {
			[nativeRecords addObject:rec];
		}
	}
	
	return nativeRecords;
}

/*
 * Class:     com_example_app_addressbook_NativeAddressBook
 * Method:    getMyUID
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT void JNICALL Java_de_janrufmonitor_macab_MacAddressBookProxy_init
(JNIEnv *env, jclass clazz)
{
	NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
	
	(*env)->GetJavaVM(env, &javaVM);
	
	ABAddressBook *book = [ABAddressBook sharedAddressBook];
	
	NSNotificationCenter *center = [NSNotificationCenter defaultCenter];
	ABNotificationObserver *observer = [ABNotificationObserver alloc];
	
	[center addObserver: observer
			   selector:@selector(notificationReceived:)
				   name: kABDatabaseChangedNotification
				 object:book];
	[center addObserver: observer
			   selector:@selector(notificationReceived:)
				   name: kABDatabaseChangedExternallyNotification
				object:book];
	
	jclass local_tester_cls = (*env)->FindClass(env,"de/janrufmonitor/macab/JNIInvocationHandler");
	if (local_tester_cls == NULL) {
		NSLog(@"init of JVM failed to obtain tester class");
		return;
	}
	else {
		/* Create a global reference */
		cls = (*env)->NewGlobalRef(env, local_tester_cls);
		
		/* The local reference is no longer useful */
		(*env)->DeleteLocalRef(env, local_tester_cls);
		
		/* Is the global reference created successfully? */
		if (cls == NULL) {
			NSLog(@"init of jvm failed to obtain global reference to tester class");
			return;
		}
		
		modifiedMethodID = (*env)->GetStaticMethodID(env, cls, "addressBookChanged", "(Ljava/lang/Object;)V");
		if (modifiedMethodID == NULL) {
			NSLog(@"Cannot obtain method ID");
			return;
		}
	}
	
	[pool release];
}


/*
 * Class:     com_example_app_addressbook_NativeAddressBook
 * Method:    getMyUID
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_de_janrufmonitor_macab_MacAddressBookProxy_getMyUID
(JNIEnv *env, jclass clazz)
{
	jstring myUID = NULL; // need to declare outside of the JNF_COCOA_ENTER/EXIT @try/catch scope
	
	JNF_COCOA_ENTER(env);
	
	NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
	
	ABPerson *me = [[ABAddressBook sharedAddressBook] me];
	myUID = JNFNSToJavaString(env, [me uniqueId]); // convert the NSString to a Java string
	
	[pool release];
	
	JNF_COCOA_EXIT(env);
	
	return myUID;
}


/*
 * Class:     com_example_app_addressbook_NativeAddressBook
 * Method:    getNativeAddressBookContacts
 * Signature: ()Ljava/util/List;
 */
JNIEXPORT jobject JNICALL Java_de_janrufmonitor_macab_MacAddressBookProxy_getNativeAddressBookRecords
(JNIEnv *env, jclass clazz)
{
    jobject javaRecords = NULL; // need to declare outside of the JNF_COCOA_ENTER/EXIT @try/catch scope
	
	JNF_COCOA_ENTER(env);
	
	NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
	
	// get all people into an array
	NSMutableArray *nativeRecords = [NSMutableArray arrayWithArray:[[ABAddressBook sharedAddressBook] people]];
	
	// add yourself, because -people does not vend the Me card.
	// Seems that -people actually does vend the Me card
	//ABPerson *me = [[ABAddressBook sharedAddressBook] me];
	//if (me != nil) [nativeRecords addObject:me];
	
	[nativeRecords addObjectsFromArray:[[ABAddressBook sharedAddressBook] groups]];
	
	javaRecords = coerceRecords(nativeRecords, env);
	
	[pool release];
	
	JNF_COCOA_EXIT(env);
	
	return javaRecords;
}

JNIEXPORT jobject JNICALL Java_de_janrufmonitor_macab_MacAddressBookProxy_getRecordsByUIDs
(JNIEnv *env, jclass clazz, jobject uids) {
	jobject javaRecords = NULL;
	
	JNF_COCOA_ENTER(env);
	NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
	
	javaRecords = coerceRecords(getRecordsByUIDs(uids, env), env);
	
	[pool release];
	JNF_COCOA_EXIT(env);
	
	return javaRecords;
}

JNIEXPORT jbyteArray JNICALL Java_de_janrufmonitor_macab_MacAddressBookProxy_getUserImage
(JNIEnv *env, jclass clazz, jstring uid) {
	jbyteArray byteArray = NULL;
	
	JNF_COCOA_ENTER(env);
	NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
	
	ABAddressBook *book = [ABAddressBook sharedAddressBook];
	ABRecord *rec = [book recordForUniqueId:JNFJavaToNSString(env, uid)];
	if([rec isKindOfClass: [ABPerson class]]) {
		ABPerson *person = (ABPerson *)rec;
		NSImage *anImage = [[NSImage alloc] initWithData: [person imageData]];
		if(anImage != nil) {
			NSArray *representations;
			NSData *jpegData;
			
			representations = [anImage representations];
			
			jpegData = [NSBitmapImageRep representationOfImageRepsInArray:representations 
																  usingType:NSPNGFileType properties:[NSDictionary dictionaryWithObject:[NSNumber numberWithBool:false] forKey:NSImageInterlaced]];
			
			if(jpegData != nil) {
				NSUInteger len = [jpegData length];
				
				byteArray = (*env)->NewByteArray(env, len);
				(*env)->SetByteArrayRegion(env, byteArray, 0, len, [jpegData bytes]);
			}
		}
	}
	
	[pool release];
	JNF_COCOA_EXIT(env);
	
	return byteArray;
}

JNIEXPORT void JNICALL Java_de_janrufmonitor_macab_MacAddressBookProxy_doBeginModification
(JNIEnv *env, jclass clazz) {
	dontSave = true;
}

JNIEXPORT void JNICALL Java_de_janrufmonitor_macab_MacAddressBookProxy_doEndModification
(JNIEnv *env, jclass clazz) {
	
	JNF_COCOA_ENTER(env);
	NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
	
	ABAddressBook *addressBook = [ABAddressBook sharedAddressBook];
	[addressBook save];
	
	[pool release];
	JNF_COCOA_EXIT(env);
	
	dontSave = false;
}

JNIEXPORT jstring JNICALL Java_de_janrufmonitor_macab_MacAddressBookProxy_addPerson
(JNIEnv *env, jclass clazz, jstring fName, jstring mName,jstring lName,jstring title, jstring org, jobject email, jobject phone, jobject address, jobject chat, jboolean isPerson, jstring groupUID) {
	jstring uid;
	
	JNF_COCOA_ENTER(env);
	
	NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
	
	ABAddressBook *addressBook;
	ABPerson *newPerson;
	
	addressBook = [ABAddressBook sharedAddressBook];
	
	newPerson = [[[ABPerson alloc] init] autorelease];
	
	[newPerson setValue:JNFJavaToNSString(env, fName)
			forProperty:kABFirstNameProperty];
	
	[newPerson setValue:JNFJavaToNSString(env, mName)
			forProperty:kABMiddleNameProperty];
	
	[newPerson setValue:JNFJavaToNSString(env, lName)
			forProperty:kABLastNameProperty];
	
	[newPerson setValue:JNFJavaToNSString(env, title)
			forProperty:kABTitleProperty];
	
	[newPerson setValue:JNFJavaToNSString(env, org)
			forProperty:kABOrganizationProperty];
	
	if(email != nil) {
		[newPerson setValue:coerceJavaMultiValue(email, env) forProperty:kABEmailProperty];
	}
	if(phone != nil) {
		[newPerson setValue:coerceJavaMultiValue(phone, env) forProperty:kABPhoneProperty];
	}
	if(address != nil) {
		[newPerson setValue:coerceJavaMultiValue(address, env) forProperty:kABAddressProperty];
	}
	if(chat != nil){
		[newPerson setValue:coerceJavaMultiValue(chat, env) forProperty:kABInstantMessageServiceICQ];
	}
	
	if(isPerson) {
		[newPerson setValue:[NSNumber numberWithLong:kABShowAsPerson] forProperty:kABPersonFlags];
	} else {
		[newPerson setValue:[NSNumber numberWithLong:kABShowAsCompany] forProperty:kABPersonFlags];
	}
	
	// Kontakt zum Adressbuch hinzufügen
	[addressBook addRecord:newPerson];
	// Eventuell Kontakt zu Gruppe hinzufügen
	if(groupUID != nil) {
		ABRecord *abRecord = [addressBook recordForUniqueId:JNFJavaToNSString(env, groupUID)];
		if(abRecord != nil && [abRecord isKindOfClass:[ABGroup class]]) {
			ABGroup *abGroup = (ABGroup *)abRecord;
			[abGroup addMember:newPerson];
		}
	}
	
	if(!dontSave) {
		[addressBook save];
	}
	
	uid = JNFNSToJavaString(env, [newPerson uniqueId]);
	
	[pool release];
	
	JNF_COCOA_EXIT(env);
	
	return uid;
}

JNIEXPORT jboolean JNICALL Java_de_janrufmonitor_macab_MacAddressBookProxy_addRecordToGroup
(JNIEnv *env, jclass clazz, jstring recordUID, jstring groupUID) {
	jboolean ret = false;
	JNF_COCOA_ENTER(env);
	
	NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
	
	ABAddressBook *addressBook = [ABAddressBook sharedAddressBook];
	
	ABRecord *shouldBeGroup = [addressBook recordForUniqueId:JNFJavaToNSString(env, groupUID)];
	
	if(shouldBeGroup != nil && [shouldBeGroup isKindOfClass:[ABGroup class]]) {
		ABGroup *group = (ABGroup *)shouldBeGroup;
		
		ABRecord *rec = [addressBook recordForUniqueId:JNFJavaToNSString(env, recordUID)];
		
		if(rec != nil) {
			if([rec isKindOfClass:[ABPerson class]]) {
				[group addMember:(ABPerson*) rec];
			} else {
				[group addSubgroup:(ABGroup*) rec];
			}
			
			if(!dontSave) {
				[addressBook save];
			}
			
			ret = true;
		}
	}
	
	[pool release];
	
	JNF_COCOA_EXIT(env);
	
	return ret;
}

JNIEXPORT jstring JNICALL Java_de_janrufmonitor_macab_MacAddressBookProxy_addGroup
(JNIEnv *env, jclass clazz, jstring name, jstring groupUID) {
	jstring uid;
	
	JNF_COCOA_ENTER(env);
	
	NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
	
	ABAddressBook *addressBook;
	ABGroup *newGroup;
	
	addressBook = [ABAddressBook sharedAddressBook];
	
	newGroup = [[[ABGroup alloc] init] autorelease];
	
	[newGroup setValue:JNFJavaToNSString(env, name)
		   forProperty:kABGroupNameProperty];
	
	// Kontakt zum Adressbuch hinzufügen
	[addressBook addRecord:newGroup];
	// Eventuell Kontakt zu Gruppe hinzufügen
	if(groupUID != nil) {
		ABRecord *abRecord = [addressBook recordForUniqueId:JNFJavaToNSString(env, groupUID)];
		if(abRecord != nil && [abRecord isKindOfClass:[ABGroup class]]) {
			ABGroup *abGroup = (ABGroup *)abRecord;
			[abGroup addSubgroup:newGroup];
		}
	}
	
	if(!dontSave) {
		[addressBook save];
	}
	
	uid = JNFNSToJavaString(env, [newGroup uniqueId]);
	
	[pool release];
	
	JNF_COCOA_EXIT(env);
	
	return uid;
}

/*
 * Class:     com_example_app_addressbook_NativeAddressBook
 * Method:    searchContact
 * Signature: (Ljava/lang/String;)Ljava/lang/String;
 */
JNIEXPORT jobject JNICALL Java_de_janrufmonitor_macab_MacAddressBookProxy_findContactsByLastName
(JNIEnv *env, jclass clazz, jstring searchString)

{
	jobject persons = NULL; // need to declare outside of the JNF_COCOA_ENTER/EXIT @try/catch scope
	
	JNF_COCOA_ENTER(env);
	
	ABSearchElement *search =getLastNameSearchElement(searchString, env);
	
	persons = findContactsBySearchElement(search, env);
	JNF_COCOA_EXIT(env);
	
	return persons;
}

/*
 * Class:     com_example_app_addressbook_NativeAddressBook
 * Method:    searchContact
 * Signature: (Ljava/lang/String;)Ljava/lang/String;
 */
JNIEXPORT jobject JNICALL Java_de_janrufmonitor_macab_MacAddressBookProxy_findContactsByFirstName
(JNIEnv *env, jclass clazz, jstring searchString)

{
	jobject persons = NULL; // need to declare outside of the JNF_COCOA_ENTER/EXIT @try/catch scope
	
	JNF_COCOA_ENTER(env);
	
	ABSearchElement *search =getFirstNameSearchElement(searchString, env);
	
	persons = findContactsBySearchElement(search, env);
	JNF_COCOA_EXIT(env);
	
	return persons;
}

/*
 * Class:     com_example_app_addressbook_NativeAddressBook
 * Method:    searchContact
 * Signature: (Ljava/lang/String;)Ljava/lang/String;
 */
JNIEXPORT jobject JNICALL Java_de_janrufmonitor_macab_MacAddressBookProxy_findContactsByNameElement
(JNIEnv *env, jclass clazz, jstring searchString)

{
	jobject persons = NULL; // need to declare outside of the JNF_COCOA_ENTER/EXIT @try/catch scope
	
	JNF_COCOA_ENTER(env);
	
	ABSearchElement *search = [ABSearchElement searchElementForConjunction:kABSearchOr 
																  children:[NSArray arrayWithObjects:getFirstNameSearchElement(searchString, env),
																			getLastNameSearchElement(searchString, env),
																			getMiddleNameSearchElement(searchString, env), nil]];
	
	persons = findContactsBySearchElement(search, env);
	JNF_COCOA_EXIT(env);
	
	return persons;
}

/*
 * Class:     com_example_app_addressbook_NativeAddressBook
 * Method:    searchContact
 * Signature: (Ljava/lang/String;)Ljava/lang/String;
 */
JNIEXPORT jobject JNICALL Java_de_janrufmonitor_macab_MacAddressBookProxy_findContactsByOrganization
(JNIEnv *env, jclass clazz, jstring searchString)

{
	jobject persons = NULL; // need to declare outside of the JNF_COCOA_ENTER/EXIT @try/catch scope
	
	JNF_COCOA_ENTER(env);
	
	ABSearchElement *search =getOrganizationSearchElement(searchString, env);
	
	persons = findContactsBySearchElement(search, env);
	JNF_COCOA_EXIT(env);
	
	return persons;
}


/*
 * Class:     com_example_app_addressbook_NativeAddressBook
 * Method:    searchContact
 * Signature: (Ljava/lang/String;)Ljava/lang/String;
 */
JNIEXPORT jobject JNICALL Java_de_janrufmonitor_macab_MacAddressBookProxy_findContactsByPhone
(JNIEnv *env, jclass clazz, jstring searchString)

{
	jobject persons = NULL; // need to declare outside of the JNF_COCOA_ENTER/EXIT @try/catch scope
	
	JNF_COCOA_ENTER(env);
	
	ABSearchElement *search =getPhoneSearchElement(searchString, env);
	
	persons = findContactsBySearchElement(search, env);
	JNF_COCOA_EXIT(env);
	
	return persons;
}


/*
 * Class:     com_example_app_addressbook_NativeAddressBook
 * Method:    searchContact
 * Signature: (Ljava/lang/String;)Ljava/lang/String;
 */
JNIEXPORT jobject JNICALL Java_de_janrufmonitor_macab_MacAddressBookProxy_findContactsByEmail
(JNIEnv *env, jclass clazz, jstring searchString)

{
	jobject persons = NULL; // need to declare outside of the JNF_COCOA_ENTER/EXIT @try/catch scope
	
	JNF_COCOA_ENTER(env);
	
	ABSearchElement *search =getEmailSearchElement(searchString, env);
	
	persons = findContactsBySearchElement(search, env);
	JNF_COCOA_EXIT(env);
	
	return persons;
}

/*
 * Class:     com_example_app_addressbook_NativeAddressBook
 * Method:    searchContact
 * Signature: (Ljava/lang/String;)Ljava/lang/String;
 */
JNIEXPORT jobject JNICALL Java_de_janrufmonitor_macab_MacAddressBookProxy_findContactsByAddress
(JNIEnv *env, jclass clazz, jstring searchString)

{
	jobject persons = NULL; // need to declare outside of the JNF_COCOA_ENTER/EXIT @try/catch scope
	
	JNF_COCOA_ENTER(env);
	
	ABSearchElement *search =getAddressNameSearchElement(searchString, env);
	
	persons = findContactsBySearchElement(search, env);
	JNF_COCOA_EXIT(env);
	
	return persons;
}

/*
 * Class:     com_example_app_addressbook_NativeAddressBook
 * Method:    searchContact
 * Signature: (Ljava/lang/String;)Ljava/lang/String;
 */
JNIEXPORT jobject JNICALL Java_de_janrufmonitor_macab_MacAddressBookProxy_findContactsByFullTextSearch
(JNIEnv *env, jclass clazz, jstring searchString)

{
	jobject persons = NULL; // need to declare outside of the JNF_COCOA_ENTER/EXIT @try/catch scope
	
	JNF_COCOA_ENTER(env);
	
	ABSearchElement *search =getFullTextSearchElement(searchString, env);
	
	persons = findContactsBySearchElement(search, env);
	JNF_COCOA_EXIT(env);
	
	return persons;
}

JNIEXPORT void JNICALL Java_de_janrufmonitor_macab_MacAddressBookProxy_revealInAddressBook
(JNIEnv *env, jclass clazz, jstring uid, jboolean edit) {
	JNF_COCOA_ENTER(env);
	
	CFStringRef urlString = CFStringCreateWithFormat(NULL, NULL, edit ? CFSTR("addressbook://%@?edit") : CFSTR("addressbook://%@"), JNFJavaToNSString(env, uid));
	CFURLRef urlRef = CFURLCreateWithString(NULL, urlString, NULL);
	LSOpenCFURLRef(urlRef, NULL);	CFRelease(urlRef);
	CFRelease(urlString);
	
	JNF_COCOA_EXIT(env);
}

// puts all the properties of an ABPerson into an NSDictionary, and uses the same 
// root coercer to transform that NSDictionary into a java.util.Map
@implementation ABPersonCoercer

+ (ABPersonCoercer *) personCoercer {
	return [[[ABPersonCoercer alloc] init] autorelease];
}

- (jobject) coerceNSObject:(id)obj withEnv:(JNIEnv *)env usingCoercer:(JNFTypeCoercion *)coercer {
	ABPerson *person = obj;
	NSMutableDictionary *dict = [NSMutableDictionary dictionary];
	
	NSMutableArray *parentGroups = [[[NSMutableArray alloc] init] autorelease];
	for(ABGroup *group in [person parentGroups]) {
		[parentGroups addObject:[group uniqueId]];
	}
	[dict setValue:parentGroups forKey:@"parentGroups"];
	
	[dict setValue:[NSNumber numberWithBool:([person imageData] != nil)] forKey:@"hasPicture"];
	
	NSArray *props = [ABPerson properties];
	for (NSString *propName in props) {
		if (propName == nil) continue;
		
		id prop = [person valueForProperty:propName]; 
		if (prop == nil) continue;
		
		[dict setValue:prop forKey:propName];
	}
	
	return [coercer coerceNSObject:dict withEnv:env usingCoercer:coercer];
}


- (id) coerceJavaObject:(jobject)obj withEnv:(JNIEnv *)env usingCoercer:(JNFTypeCoercion *)coercer {
	return nil; // exercise left to the reader
}

@end

// puts all the properties of an ABPerson into an NSDictionary, and uses the same 
// root coercer to transform that NSDictionary into a java.util.Map
@implementation DateCoercer

+ (DateCoercer *) dateCoercer {
	return [[[DateCoercer alloc] init] autorelease];
}

- (jobject) coerceNSObject:(id)obj withEnv:(JNIEnv *)env usingCoercer:(JNFTypeCoercion *)coercer {
	NSDate *date = obj;
	
	NSString *dateString = [date description]; 
	
	return [coercer coerceNSObject:dateString withEnv:env usingCoercer:coercer];
}

- (id) coerceJavaObject:(jobject)obj withEnv:(JNIEnv *)env usingCoercer:(JNFTypeCoercion *)coercer {
	return nil; // exercise left to the reader
}

@end

@implementation NSNotificationCoercer

+ (NSNotificationCoercer *) notificationCoercer {
	return [[[NSNotificationCoercer alloc] init] autorelease];
}

- (jobject) coerceNSObject:(id)obj withEnv:(JNIEnv *)env usingCoercer:(JNFTypeCoercion *)coercer {
	NSNotification *notification = obj;
	NSDictionary *dic = [notification userInfo];
	
	return [coercer coerceNSObject:dic withEnv:env usingCoercer:coercer];
}

- (id) coerceJavaObject:(jobject)obj withEnv:(JNIEnv *)env usingCoercer:(JNFTypeCoercion *)coercer {
	return nil; // exercise left to the reader
}
@end

// creates an NSArray for each ABMultiValue, and puts each key/value pair into its own NSDictionary,
// then uses the same root coercer to transform them into java.util.Lists and java.util.Maps
@implementation ABMultiValueCoercer

+ (ABMultiValueCoercer *) multiValueCoercer {
	return [[[ABMultiValueCoercer alloc] init] autorelease];
}

- (jobject) coerceNSObject:(id)obj withEnv:(JNIEnv *)env usingCoercer:(JNFTypeCoercion *)coercer {
	ABMultiValue *multiValue = obj;
	
	NSUInteger count = [multiValue count];
	NSMutableArray *values = [NSMutableArray arrayWithCapacity:count];
	
	NSUInteger i;
	for (i = 0; i < count; i++) {
		NSString *label = [multiValue labelAtIndex:i];
		id object = [multiValue valueAtIndex:i];
		
		[values addObject:[NSDictionary dictionaryWithObjects:&object forKeys:&label count:1]];
	}
	
	return [coercer coerceNSObject:values withEnv:env usingCoercer:coercer];
}


- (id) coerceJavaObject:(jobject)obj withEnv:(JNIEnv *)env usingCoercer:(JNFTypeCoercion *)coercer {
	return nil; // exercise left to the reader
}
@end


// creates an NSArray for each ABMultiValue, and puts each key/value pair into its own NSDictionary,
// then uses the same root coercer to transform them into java.util.Lists and java.util.Maps
@implementation ABGroupCoercer

+ (ABGroupCoercer *) groupCoercer {
	return [[[ABGroupCoercer alloc] init] autorelease];
}

- (jobject) coerceNSObject:(id)obj withEnv:(JNIEnv *)env usingCoercer:(JNFTypeCoercion *)coercer {
	ABGroup *group = obj;
	
	NSMutableArray *values = [[[NSMutableArray alloc] init] autorelease];
	
	NSMutableArray *parentGroups = [[[NSMutableArray alloc] init] autorelease];
	for(ABGroup *g in [group parentGroups]) {
		[parentGroups addObject:[g uniqueId]];
	}
	
	NSMutableArray *subGroups = [[[NSMutableArray alloc] init] autorelease];
	for(ABGroup *g in [group subgroups]) {
		[subGroups addObject:[g uniqueId]];
	}
	
	NSMutableArray *contacts = [[[NSMutableArray alloc] init] autorelease];
	for(ABPerson *p in [group members]) {
		[contacts addObject:[p uniqueId]];
	}
	
	[values addObject:[group valueForProperty:kABGroupNameProperty]];
	[values addObject:[group valueForProperty:kABUIDProperty]];
	[values addObject:subGroups];
	[values addObject:parentGroups];
	[values addObject:contacts];
	
	return [coercer coerceNSObject:values withEnv:env usingCoercer:coercer];
}


- (id) coerceJavaObject:(jobject)obj withEnv:(JNIEnv *)env usingCoercer:(JNFTypeCoercion *)coercer {
	return nil; // exercise left to the reader
}
@end

@implementation ABNotificationObserver
- (void) notificationReceived:(NSNotification *)notification {
	NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];
	
	JNIEnv *env;
	bool shouldDetach = false;
	
	if (GetJNIEnv(&env, &shouldDetach) != JNI_OK) {
		NSLog(@"recordChanged: could not attach to JVM");
		return;
	}
	
	// create and load a coercer with all of the different coercions to convert each type of object
	JNFTypeCoercer *coercer = [[[JNFTypeCoercer alloc] init] autorelease];
	[JNFDefaultCoercions addStringCoercionTo:coercer];
	[JNFDefaultCoercions addNumberCoercionTo:coercer];
	[JNFDefaultCoercions addListCoercionTo:coercer];
	[JNFDefaultCoercions addMapCoercionTo:coercer];
	[JNFDefaultCoercions addSetCoercionTo:coercer];
	[coercer addCoercion:[NSNotificationCoercer notificationCoercer] forNSClass:[NSNotification class] javaClass:nil];
	
	jvalue *args = (jvalue *)calloc(1, sizeof(jvalue));
	jvalue arg;
	arg.l = [coercer coerceNSObject:notification withEnv:env];
	args[0] = arg;
	
	(*env)->CallStaticVoidMethodA(env, cls, modifiedMethodID, args);
	
	if (shouldDetach) {
		(*javaVM)->DetachCurrentThread(javaVM);
	}
	
	[pool release];
 }
@end
