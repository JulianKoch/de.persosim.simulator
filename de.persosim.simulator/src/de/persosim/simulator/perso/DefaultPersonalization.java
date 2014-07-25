package de.persosim.simulator.perso;

import static de.persosim.simulator.utils.PersoSimLogger.TRACE;
import static de.persosim.simulator.utils.PersoSimLogger.log;

import java.io.IOException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import de.persosim.simulator.cardobjects.AuthObjectIdentifier;
import de.persosim.simulator.cardobjects.ByteDataAuxObject;
import de.persosim.simulator.cardobjects.CardFile;
import de.persosim.simulator.cardobjects.ChangeablePasswordAuthObject;
import de.persosim.simulator.cardobjects.DateAuxObject;
import de.persosim.simulator.cardobjects.DateTimeCardObject;
import de.persosim.simulator.cardobjects.DateTimeObjectIdentifier;
import de.persosim.simulator.cardobjects.DedicatedFile;
import de.persosim.simulator.cardobjects.DedicatedFileIdentifier;
import de.persosim.simulator.cardobjects.DomainParameterSetCardObject;
import de.persosim.simulator.cardobjects.DomainParameterSetIdentifier;
import de.persosim.simulator.cardobjects.ElementaryFile;
import de.persosim.simulator.cardobjects.FileIdentifier;
import de.persosim.simulator.cardobjects.Iso7816LifeCycleState;
import de.persosim.simulator.cardobjects.KeyIdentifier;
import de.persosim.simulator.cardobjects.KeyObject;
import de.persosim.simulator.cardobjects.MasterFile;
import de.persosim.simulator.cardobjects.MrzAuthObject;
import de.persosim.simulator.cardobjects.OidIdentifier;
import de.persosim.simulator.cardobjects.PasswordAuthObject;
import de.persosim.simulator.cardobjects.PasswordAuthObjectWithRetryCounter;
import de.persosim.simulator.cardobjects.PinObject;
import de.persosim.simulator.cardobjects.ShortFileIdentifier;
import de.persosim.simulator.cardobjects.TrustPointCardObject;
import de.persosim.simulator.cardobjects.TrustPointIdentifier;
import de.persosim.simulator.crypto.DomainParameterSet;
import de.persosim.simulator.crypto.StandardizedDomainParameters;
import de.persosim.simulator.crypto.certificates.CardVerifiableCertificate;
import de.persosim.simulator.exception.CertificateNotParseableException;
import de.persosim.simulator.protocols.Protocol;
import de.persosim.simulator.protocols.TR03110;
import de.persosim.simulator.protocols.auxVerification.AuxProtocol;
import de.persosim.simulator.protocols.ca.Ca;
import de.persosim.simulator.protocols.ca.CaProtocol;
import de.persosim.simulator.protocols.file.FileProtocol;
import de.persosim.simulator.protocols.pace.Pace;
import de.persosim.simulator.protocols.pace.PaceProtocol;
import de.persosim.simulator.protocols.pin.PinProtocol;
import de.persosim.simulator.protocols.ri.Ri;
import de.persosim.simulator.protocols.ri.RiOid;
import de.persosim.simulator.protocols.ri.RiProtocol;
import de.persosim.simulator.protocols.ta.CertificateRole;
import de.persosim.simulator.protocols.ta.RelativeAuthorization;
import de.persosim.simulator.protocols.ta.TaOid;
import de.persosim.simulator.protocols.ta.TaProtocol;
import de.persosim.simulator.protocols.ta.TerminalType;
import de.persosim.simulator.secstatus.NullSecurityCondition;
import de.persosim.simulator.secstatus.PaceSecurityCondition;
import de.persosim.simulator.secstatus.PaceWithPasswordSecurityCondition;
import de.persosim.simulator.secstatus.SecCondition;
import de.persosim.simulator.secstatus.TaSecurityCondition;
import de.persosim.simulator.tlv.Asn1;
import de.persosim.simulator.tlv.ConstructedTlvDataObject;
import de.persosim.simulator.tlv.TlvDataObject;
import de.persosim.simulator.tlv.TlvDataObjectContainer;
import de.persosim.simulator.tlv.TlvTag;
import de.persosim.simulator.utils.BitField;
import de.persosim.simulator.utils.HexString;

/**
 * Personalization used as default.
 * <p/>
 * This personalization is intended to be as close to the currently available nPA
 * as possible. During development the closest already supported configuration
 * is used.
 * 
 * @author amay
 * 
 */
public class DefaultPersonalization implements Personalization, Pace {

	protected List<Protocol> protocols = null;

	@Override
	public MasterFile getObjectTree() {
		try {
			MasterFile mf = new MasterFile(new FileIdentifier(0x3F00), new DedicatedFileIdentifier(new byte[] { (byte) 0xA0,
							0x0, 0x0, 0x2, 0x47, 0x10, 0x03 }));
			
			
			MrzAuthObject mrz = new MrzAuthObject (new AuthObjectIdentifier(1),
					"P<D<<C11T002JM4<<<<<<<<<<<<<<<9608122F2310314D<<<<<<<<<<<<<4MUSTERMANN<<ERIKA<<<<<<<<<<<<<");
			mf.addChild(mrz);
			
			ChangeablePasswordAuthObject can = new ChangeablePasswordAuthObject(new AuthObjectIdentifier(2), "500540".getBytes("UTF-8"), "CAN", 6, 6);
			can.updateLifeCycleState(Iso7816LifeCycleState.OPERATIONAL_ACTIVATED);
			mf.addChild(can);
			
			PasswordAuthObjectWithRetryCounter pin = new PinObject(
					new AuthObjectIdentifier(3), "123456".getBytes("UTF-8"), 6, 6, 3);
			pin.updateLifeCycleState(Iso7816LifeCycleState.OPERATIONAL_ACTIVATED);
			mf.addChild(pin);
			
			PasswordAuthObject puk = new PasswordAuthObject(
					new AuthObjectIdentifier(4),
					"9876543210".getBytes("UTF-8"), "PUK");
			mf.addChild(puk);

			//PACE security conditions
			SecCondition paceWithPin = new PaceWithPasswordSecurityCondition(pin);
			SecCondition pace = new PaceSecurityCondition();
			SecCondition unprotected = new NullSecurityCondition();

			HashSet<SecCondition> paceWithPinSet = new HashSet<>();
			paceWithPinSet.add(paceWithPin);
			HashSet<SecCondition> paceSet = new HashSet<>();
			paceSet.add(pace);
			HashSet<SecCondition> unprotectedSet = new HashSet<>();
			unprotectedSet.add(unprotected);
			HashSet<SecCondition> emptySet = new HashSet<>();

			//TA security conditions
			BitField taIsDg3 = new BitField(6).flipBit(0);
			BitField taIsDg4 = new BitField(6).flipBit(1);
			BitField taAtPrivileged = new BitField(6).flipBit(3);
			SecCondition ta = new TaSecurityCondition(null, null);
			SecCondition taWithIs = new TaSecurityCondition(TerminalType.IS, null);
			SecCondition taWithIsDg3 = new TaSecurityCondition(TerminalType.IS, new RelativeAuthorization(CertificateRole.TERMINAL, taIsDg3));
			SecCondition taWithIsDg4 = new TaSecurityCondition(TerminalType.IS, new RelativeAuthorization(CertificateRole.TERMINAL, taIsDg4));
			SecCondition taWithAtPrivileged = new TaSecurityCondition(TerminalType.AT, new RelativeAuthorization(CertificateRole.TERMINAL, taAtPrivileged));
			

			HashSet<SecCondition> taWithIsDg3Set = new HashSet<>();
			taWithIsDg3Set.add(taWithIsDg3);
			HashSet<SecCondition> taWithIsDg4Set = new HashSet<>();
			taWithIsDg4Set.add(taWithIsDg4);
			HashSet<SecCondition> taWithIsSet = new HashSet<>();
			taWithIsSet.add(taWithIs);
			HashSet<SecCondition> taSet = new HashSet<>();
			taSet.add(ta);
			HashSet<SecCondition> taForChipSecuritySet = new HashSet<>();
			taForChipSecuritySet.add(taWithAtPrivileged);
			taForChipSecuritySet.add(taWithIs);
			
			//define {@link OidIdentifier}
			
			
			//create domain parameters
//			DomainParameterSet domainParameterSet0 = StandardizedDomainParameters.getDomainParameterSetById(0);
			DomainParameterSet domainParameterSet13 = StandardizedDomainParameters.getDomainParameterSetById(13);
			
//			DomainParameterSetIdentifier domainParameterSetId0 = new DomainParameterSetIdentifier(0);
			DomainParameterSetIdentifier domainParameterSetId13 = new DomainParameterSetIdentifier(13);
			
//			DomainParameterSetCardObject domainParameterSetCardObject0 = new DomainParameterSetCardObject(domainParameterSet0, domainParameterSetId0);
			DomainParameterSetCardObject domainParameterSetCardObject13 = new DomainParameterSetCardObject(domainParameterSet13, domainParameterSetId13);
			
//			mf.addChild(domainParameterSetCardObject0);
			mf.addChild(domainParameterSetCardObject13);
			
			//register domain parameters for Pace
			domainParameterSetCardObject13.addOidIdentifier(OID_IDENTIFIER_id_PACE_ECDH_GM_3DES_CBC_CBC);
			domainParameterSetCardObject13.addOidIdentifier(OID_IDENTIFIER_id_PACE_ECDH_GM_AES_CBC_CMAC_128);
			domainParameterSetCardObject13.addOidIdentifier(OID_IDENTIFIER_id_PACE_ECDH_GM_AES_CBC_CMAC_192);
			domainParameterSetCardObject13.addOidIdentifier(OID_IDENTIFIER_id_PACE_ECDH_GM_AES_CBC_CMAC_256);
			
			domainParameterSetCardObject13.addOidIdentifier(OID_IDENTIFIER_id_PACE_ECDH_IM_3DES_CBC_CBC);
			domainParameterSetCardObject13.addOidIdentifier(OID_IDENTIFIER_id_PACE_ECDH_IM_AES_CBC_CMAC_128);
			domainParameterSetCardObject13.addOidIdentifier(OID_IDENTIFIER_id_PACE_ECDH_IM_AES_CBC_CMAC_192);
			domainParameterSetCardObject13.addOidIdentifier(OID_IDENTIFIER_id_PACE_ECDH_IM_AES_CBC_CMAC_256);
			
			//register domain parameters for CA
//			domainParameterSetCardObject0.addOidIdentifier(Ca.OID_IDENTIFIER_id_CA_DH_3DES_CBC_CBC);
//			domainParameterSetCardObject0.addOidIdentifier(Ca.OID_IDENTIFIER_id_CA_DH_AES_CBC_CMAC_128);
//			domainParameterSetCardObject0.addOidIdentifier(Ca.OID_IDENTIFIER_id_CA_DH_AES_CBC_CMAC_192);
//			domainParameterSetCardObject0.addOidIdentifier(Ca.OID_IDENTIFIER_id_CA_DH_AES_CBC_CMAC_256);
			
			domainParameterSetCardObject13.addOidIdentifier(Ca.OID_IDENTIFIER_id_CA_ECDH_3DES_CBC_CBC);
			domainParameterSetCardObject13.addOidIdentifier(Ca.OID_IDENTIFIER_id_CA_ECDH_AES_CBC_CMAC_128);
			domainParameterSetCardObject13.addOidIdentifier(Ca.OID_IDENTIFIER_id_CA_ECDH_AES_CBC_CMAC_192);
			domainParameterSetCardObject13.addOidIdentifier(Ca.OID_IDENTIFIER_id_CA_ECDH_AES_CBC_CMAC_256);
			
			//CA static key pair PICC
			DomainParameterSet domainParameterSet = StandardizedDomainParameters.getDomainParameterSetById(13);
			byte[] publicKeyMaterial = HexString.toByteArray("04 A4 4E BE 54 51 DF 7A AD B0 1E 45 9B 8C 92 8A 87 74 6A 57 92 7C 8C 28 A6 77 5C 97 A7 E1 FE 8D 9A 46 FF 4A 1C C7 E4 D1 38 9A EA 19 75 8E 4F 75 C2 8C 59 8F D7 34 AE BE B1 35 33 7C F9 5B E1 2E 94");
			byte[] privateKeyMaterial = HexString.toByteArray("79 84 67 4C F3 B3 A5 24 BF 92 9C E8 A6 7F CF 22 17 3D A0 BA D5 95 EE D6 DE B7 2D 22 C5 42 FA 9D");
			
			PublicKey publicKey = domainParameterSet.reconstructPublicKey(publicKeyMaterial);
			PrivateKey privateKey = domainParameterSet.reconstructPrivateKey(privateKeyMaterial);
			KeyPair keyPair = new KeyPair(publicKey, privateKey);
			
			
			KeyObject caKey = new KeyObject(keyPair, new KeyIdentifier(2));
			caKey.addOidIdentifier(Ca.OID_IDENTIFIER_id_CA_ECDH_3DES_CBC_CBC);
			caKey.addOidIdentifier(Ca.OID_IDENTIFIER_id_CA_ECDH_AES_CBC_CMAC_128);
			caKey.addOidIdentifier(Ca.OID_IDENTIFIER_id_CA_ECDH_AES_CBC_CMAC_192);
			caKey.addOidIdentifier(Ca.OID_IDENTIFIER_id_CA_ECDH_AES_CBC_CMAC_256);
			mf.addChild(caKey);
			
			//RI static key pair PICC
			domainParameterSet = StandardizedDomainParameters.getDomainParameterSetById(13);
			publicKeyMaterial = HexString.toByteArray("04 5D 3B 49 C8 EE 25 02 95 F7 C0 EF 6A 1A E8 10 C4 B9 E1 F5 F8 0D 31 6D C9 AD AD 16 08 0C 17 84 CF 88 1E E0 A3 75 BC A1 B5 3C 98 F3 AC39 FD 0C A9 0C E3 1D 2D 82 76 D3 CF B3 2B 31 6B A0 22 10 23 ");
			privateKeyMaterial = HexString.toByteArray("4E 0C 36 7E 36 CE 7E 8E 37 9E F5 AE 0C 88 59 C7 51 2D 8B EB 61 92 3F ED F7 F6 02 04 B1 95 8A 8C ");
			
			publicKey = domainParameterSet.reconstructPublicKey(publicKeyMaterial);
			privateKey = domainParameterSet.reconstructPrivateKey(privateKeyMaterial);
			keyPair = new KeyPair(publicKey, privateKey);
			
			
			KeyObject riKey = new KeyObject(keyPair, new KeyIdentifier(1));
			riKey.addOidIdentifier(new OidIdentifier(new RiOid(Ri.id_RI_ECDH)));
			mf.addChild(riKey);
			
			//XXX check why the following file is there
			ElementaryFile file1 = new ElementaryFile(
					new FileIdentifier(0x011A),
					new ShortFileIdentifier(1), new byte[] { (byte) 0xFF,
							0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14,
							15 }, paceSet, paceWithPinSet, emptySet);
			mf.addChild(file1);

			//EF.CardAccess
			byte[] efCardAccessContentPlain = HexString.toByteArray("31 81 80 30 0D 06 08 04 00 7F 00 07 02 02 02 02 01 02 30 0F 06 0A 04 00 7F 00 07 02 02 03 02 02 02 01 02 30 12 06 0A 04 00 7F 00 07 02 02 04 02 02 02 01 02 02 01 0D 30 19 06 09 04 00 7F 00 07 02 02 03 02 30 0C 06 07 04 00 7F 00 07 01 02 02 01 0D 30 2F 06 08 04 00 7F 00 07 02 02 06 16 23 68 74 74 70 73 3A 2F 2F 77 77 77 2E 68 6A 70 2D 63 6F 6E 73 75 6C 74 69 6E 67 2E 63 6F 6D 2F 68 6F 6D 65");
			TlvDataObjectContainer efCardAccessContent = new TlvDataObjectContainer(efCardAccessContentPlain);
			log(getClass(), "EF.CardAccess of length " + efCardAccessContent.getLength() + " is " + efCardAccessContent, TRACE);
			ElementaryFile efCardAccess = new ElementaryFile (
					new FileIdentifier(0x011C),
					new ShortFileIdentifier(0x1C), efCardAccessContent
					.toByteArray(), unprotectedSet, emptySet, emptySet);
			mf.addChild(efCardAccess);
			
			//EF.CardSecurity
			//XXX EF.CardSecurity should be generated from protocol data as well
			//XXX EF.CardSecurity wrong access conditions
			byte[] efCardSecurityContentPlain = HexString
					.toByteArray("30 82 05 59 06 09 2A 86 48 86 F7 0D 01 07 02 A0 82 05 4A 30 82 05 46 02 01 03 31 0F 30 0D 06 09 60 86 48 01 65 03 04 02 01 05 00 30 82 01 44 06 08 04 00 7F 00 07 03 02 01 A0 82 01 36 04 82 01 32 31 82 01 2E 30 0D 06 08 04 00 7F 00 07 02 02 02 02 01 02 30 0F 06 0A 04 00 7F 00 07 02 02 03 02 02 02 01 02 30 12 06 0A 04 00 7F 00 07 02 02 04 02 02 02 01 02 02 01 0D 30 19 06 09 04 00 7F 00 07 02 02 03 02 30 0C 06 07 04 00 7F 00 07 01 02 02 01 0D 30 2F 06 08 04 00 7F 00 07 02 02 06 16 23 68 74 74 70 73 3A 2F 2F 77 77 77 2E 68 6A 70 2D 63 6F 6E 73 75 6C 74 69 6E 67 2E 63 6F 6D 2F 68 6F 6D 65 30 17 06 0A 04 00 7F 00 07 02 02 05 02 03 30 09 02 01 01 02 01 01 01 01 00 30 17 06 0A 04 00 7F 00 07 02 02 05 02 03 30 09 02 01 01 02 01 02 01 01 FF 30 19 06 09 04 00 7F 00 07 02 02 05 02 30 0C 06 07 04 00 7F 00 07 01 02 02 01 0D 30 5F 06 09 04 00 7F 00 07 02 02 01 02 30 52 30 0C 06 07 04 00 7F 00 07 01 02 02 01 0D 03 42 00 04 A4 4E BE 54 51 DF 7A AD B0 1E 45 9B 8C 92 8A 87 74 6A 57 92 7C 8C 28 A6 77 5C 97 A7 E1 FE 8D 9A 46 FF 4A 1C C7 E4 D1 38 9A EA 19 75 8E 4F 75 C2 8C 59 8F D7 34 AE BE B1 35 33 7C F9 5B E1 2E 94 A0 82 02 CC 30 82 02 C8 30 82 02 6F A0 03 02 01 02 02 06 01 45 C9 66 61 27 30 0A 06 08 2A 86 48 CE 3D 04 03 02 30 53 31 0B 30 09 06 03 55 04 06 13 02 44 45 31 17 30 15 06 03 55 04 0A 0C 0E 48 4A 50 20 43 6F 6E 73 75 6C 74 69 6E 67 31 17 30 15 06 03 55 04 0B 0C 0E 43 6F 75 6E 74 72 79 20 53 69 67 6E 65 72 31 12 30 10 06 03 55 04 03 0C 09 48 4A 50 20 50 42 20 43 53 30 1E 17 0D 31 34 30 35 30 34 32 32 34 31 34 31 5A 17 0D 31 35 30 34 32 39 32 32 34 31 34 31 5A 30 54 31 0B 30 09 06 03 55 04 06 13 02 44 45 31 17 30 15 06 03 55 04 0A 0C 0E 48 4A 50 20 43 6F 6E 73 75 6C 74 69 6E 67 31 18 30 16 06 03 55 04 0B 0C 0F 44 6F 63 75 6D 65 6E 74 20 53 69 67 6E 65 72 31 12 30 10 06 03 55 04 03 0C 09 48 4A 50 20 50 42 20 44 53 30 82 01 33 30 81 EC 06 07 2A 86 48 CE 3D 02 01 30 81 E0 02 01 01 30 2C 06 07 2A 86 48 CE 3D 01 01 02 21 00 A9 FB 57 DB A1 EE A9 BC 3E 66 0A 90 9D 83 8D 72 6E 3B F6 23 D5 26 20 28 20 13 48 1D 1F 6E 53 77 30 44 04 20 7D 5A 09 75 FC 2C 30 57 EE F6 75 30 41 7A FF E7 FB 80 55 C1 26 DC 5C 6C E9 4A 4B 44 F3 30 B5 D9 04 20 26 DC 5C 6C E9 4A 4B 44 F3 30 B5 D9 BB D7 7C BF 95 84 16 29 5C F7 E1 CE 6B CC DC 18 FF 8C 07 B6 04 41 04 8B D2 AE B9 CB 7E 57 CB 2C 4B 48 2F FC 81 B7 AF B9 DE 27 E1 E3 BD 23 C2 3A 44 53 BD 9A CE 32 62 54 7E F8 35 C3 DA C4 FD 97 F8 46 1A 14 61 1D C9 C2 77 45 13 2D ED 8E 54 5C 1D 54 C7 2F 04 69 97 02 21 00 A9 FB 57 DB A1 EE A9 BC 3E 66 0A 90 9D 83 8D 71 8C 39 7A A3 B5 61 A6 F7 90 1E 0E 82 97 48 56 A7 02 01 01 03 42 00 04 85 06 CE 37 26 60 47 19 D7 55 BF 58 CF 7E F7 BD AB B5 C1 B4 EC DD 47 66 48 0F 76 53 2D 54 A5 2C 85 56 B5 6C 32 D9 91 6D EE 1C 8D 12 3E 76 FB 15 91 94 9B 69 E1 54 1A 1D 4A 4A 7C C4 C1 E0 AD DD A3 52 30 50 30 1F 06 03 55 1D 23 04 18 30 16 80 14 30 EE 98 DE F4 27 9E AB 94 E8 90 CB 0F ED 54 37 5B 89 8D CC 30 1D 06 03 55 1D 0E 04 16 04 14 27 AD F1 63 0C 61 3B 0B 62 28 96 6E 9D E3 E9 49 35 7A A6 16 30 0E 06 03 55 1D 0F 01 01 FF 04 04 03 02 07 80 30 0A 06 08 2A 86 48 CE 3D 04 03 02 03 47 00 30 44 02 20 10 E8 EF 92 D3 B0 5F A7 5E CF D3 83 EE 64 FE 95 AF 9F AA 62 1B 19 BB 01 04 E2 E8 43 28 93 1B C4 02 20 7C 21 46 87 3C 5F C1 7F 73 BD A9 90 DF 25 32 BF FA 22 60 3C 83 86 E6 18 2E 56 3C 1E CD 37 33 D5 31 82 01 16 30 82 01 12 02 01 01 30 5D 30 53 31 0B 30 09 06 03 55 04 06 13 02 44 45 31 17 30 15 06 03 55 04 0A 0C 0E 48 4A 50 20 43 6F 6E 73 75 6C 74 69 6E 67 31 17 30 15 06 03 55 04 0B 0C 0E 43 6F 75 6E 74 72 79 20 53 69 67 6E 65 72 31 12 30 10 06 03 55 04 03 0C 09 48 4A 50 20 50 42 20 43 53 02 06 01 45 C9 66 61 27 30 0D 06 09 60 86 48 01 65 03 04 02 01 05 00 A0 4A 30 17 06 09 2A 86 48 86 F7 0D 01 09 03 31 0A 06 08 04 00 7F 00 07 03 02 01 30 2F 06 09 2A 86 48 86 F7 0D 01 09 04 31 22 04 20 29 2D A3 44 46 FB 05 32 E0 1D 51 CC 37 31 88 19 D9 DD EA AD 0D 49 6A D4 34 D0 11 88 DD 2E 9D 73 30 0A 06 08 2A 86 48 CE 3D 04 03 02 04 47 30 45 02 21 00 9A C3 ED C4 FC 4B FB 2D FF F6 E3 B3 56 5B E8 01 8D 93 8B FA 83 AB F6 5D 94 29 02 8C 2F 44 7D 34 02 20 1E 30 F4 73 6E 36 2B 55 91 CA 53 2F 6D FF 82 77 9A 8A 22 A4 BE B1 0F 54 B8 23 A4 D5 C1 57 A2 42");
			
			TlvDataObjectContainer efCardSecurityContent = new TlvDataObjectContainer(efCardSecurityContentPlain);
			log(getClass(), "EF.CardSecurity of length " + efCardSecurityContent.getLength() + " is " + efCardSecurityContent, TRACE);
			ElementaryFile efCardSecurity = new ElementaryFile(
					new FileIdentifier(0x011D),
					new ShortFileIdentifier(0x1D), efCardSecurityContent
					.toByteArray(), taSet, emptySet, emptySet);
			mf.addChild(efCardSecurity);
			
			//EF.ChipSecurity
			//XXX EF.ChipSecurity should be generated from protocol data as well
			//XXX EF.ChipSecurity wrong access conditions
			byte[] efChipSecurityContentPlain = HexString
					.toByteArray("3082055806092A864886F70D010702A082054930820545020103310F300D0609608648016503040201050030820144060804007F0007030201A0820136048201323182012E300D060804007F0007020202020102300F060A04007F000702020302020201023012060A04007F0007020204020202010202010D3019060904007F000702020302300C060704007F0007010202010D302F060804007F0007020206162368747470733A2F2F7777772E686A702D636F6E73756C74696E672E636F6D2F686F6D653017060A04007F0007020205020330090201010201010101003017060A04007F0007020205020330090201010201020101FF3019060904007F000702020502300C060704007F0007010202010D305F060904007F0007020201023052300C060704007F0007010202010D03420004A44EBE5451DF7AADB01E459B8C928A87746A57927C8C28A6775C97A7E1FE8D9A46FF4A1CC7E4D1389AEA19758E4F75C28C598FD734AEBEB135337CF95BE12E94A08202CC308202C83082026FA00302010202060145C9666127300A06082A8648CE3D0403023053310B300906035504061302444531173015060355040A0C0E484A5020436F6E73756C74696E6731173015060355040B0C0E436F756E747279205369676E65723112301006035504030C09484A50205042204353301E170D3134303530343232343134315A170D3135303432393232343134315A3054310B300906035504061302444531173015060355040A0C0E484A5020436F6E73756C74696E6731183016060355040B0C0F446F63756D656E74205369676E65723112301006035504030C09484A50205042204453308201333081EC06072A8648CE3D02013081E0020101302C06072A8648CE3D0101022100A9FB57DBA1EEA9BC3E660A909D838D726E3BF623D52620282013481D1F6E5377304404207D5A0975FC2C3057EEF67530417AFFE7FB8055C126DC5C6CE94A4B44F330B5D9042026DC5C6CE94A4B44F330B5D9BBD77CBF958416295CF7E1CE6BCCDC18FF8C07B60441048BD2AEB9CB7E57CB2C4B482FFC81B7AFB9DE27E1E3BD23C23A4453BD9ACE3262547EF835C3DAC4FD97F8461A14611DC9C27745132DED8E545C1D54C72F046997022100A9FB57DBA1EEA9BC3E660A909D838D718C397AA3B561A6F7901E0E82974856A7020101034200048506CE3726604719D755BF58CF7EF7BDABB5C1B4ECDD4766480F76532D54A52C8556B56C32D9916DEE1C8D123E76FB1591949B69E1541A1D4A4A7CC4C1E0ADDDA3523050301F0603551D2304183016801430EE98DEF4279EAB94E890CB0FED54375B898DCC301D0603551D0E0416041427ADF1630C613B0B6228966E9DE3E949357AA616300E0603551D0F0101FF040403020780300A06082A8648CE3D0403020347003044022010E8EF92D3B05FA75ECFD383EE64FE95AF9FAA621B19BB0104E2E84328931BC402207C2146873C5FC17F73BDA990DF2532BFFA22603C8386E6182E563C1ECD3733D53182011530820111020101305D3053310B300906035504061302444531173015060355040A0C0E484A5020436F6E73756C74696E6731173015060355040B0C0E436F756E747279205369676E65723112301006035504030C09484A5020504220435302060145C9666127300D06096086480165030402010500A04A301706092A864886F70D010903310A060804007F0007030201302F06092A864886F70D01090431220420292DA34446FB0532E01D51CC37318819D9DDEAAD0D496AD434D01188DD2E9D73300A06082A8648CE3D0403020446304402205C26EA511901CFA8287F0D181640288263FE8B3049B2ACB031C4BCB8E4E77556022072F5554AA9C5C502CFD169455BD6DED6F37464C4E73BB0D6D210CAD3DBCA4DB9");
			
			TlvDataObjectContainer efChipSecurityContent = new TlvDataObjectContainer(efChipSecurityContentPlain);
			log(getClass(), "EF.ChipSecurity of length " + efChipSecurityContent.getLength() + " is " + efChipSecurityContent, TRACE);
			ElementaryFile efChipSecurity = new ElementaryFile(
					new FileIdentifier(0x011B),
					new ShortFileIdentifier(0x1B), efChipSecurityContent
					.toByteArray(), taForChipSecuritySet, emptySet, emptySet);
			mf.addChild(efChipSecurity);
			
			//EF.DIR
			byte[] efDIRContentPlain = HexString
					.toByteArray("61324F0FE828BD080FA000000167455349474E500F434941207A752044462E655369676E5100730C4F0AA000000167455349474E61094F07A0000002471001610B4F09E80704007F00070302610C4F0AA000000167455349474E");
			
			TlvDataObjectContainer efDIRContent = new TlvDataObjectContainer(efDIRContentPlain);
			log(getClass(), "EF.DIR of length " + efDIRContent.getLength() + " is " + efDIRContent, TRACE);
			ElementaryFile efDIR = new ElementaryFile(
					new FileIdentifier(0x2F00),
					new ShortFileIdentifier(0x1E), efDIRContent
					.toByteArray(), unprotectedSet, emptySet, emptySet);
			mf.addChild(efDIR);
			
			
			//certificates
			byte [] cvcaIsData = HexString.toByteArray("7F218201B07F4E8201685F290100420D444549534356434130303030317F4982011D060A04007F000702020202038120A9FB57DBA1EEA9BC3E660A909D838D726E3BF623D52620282013481D1F6E537782207D5A0975FC2C3057EEF67530417AFFE7FB8055C126DC5C6CE94A4B44F330B5D9832026DC5C6CE94A4B44F330B5D9BBD77CBF958416295CF7E1CE6BCCDC18FF8C07B68441048BD2AEB9CB7E57CB2C4B482FFC81B7AFB9DE27E1E3BD23C23A4453BD9ACE3262547EF835C3DAC4FD97F8461A14611DC9C27745132DED8E545C1D54C72F0469978520A9FB57DBA1EEA9BC3E660A909D838D718C397AA3B561A6F7901E0E82974856A78641045889BF5306189ABB7FA3AD0E922443F9C60162E8215053B72812663E5D798EE05097C4DFAC7470701A5B644AAEAFE1E50BA1D0ED5769151EC476C154BB4A56848701015F200D444549534356434130303030317F4C0E060904007F0007030102015301E35F25060104000500055F24060105000500055F37400A589134205376E20EFF49E108560F1CB47C7D221E96E51FF3C6F4EAF1F6CCC000A5E34ED8E3F6E05253DA09B0D68FF5DFB5BD586782B987453C655FBEE8EC59");
			byte [] cvcaAtData = HexString.toByteArray("7F218201B47F4E82016C5F290100420D444541544356434130303030317F4982011D060A04007F000702020202038120A9FB57DBA1EEA9BC3E660A909D838D726E3BF623D52620282013481D1F6E537782207D5A0975FC2C3057EEF67530417AFFE7FB8055C126DC5C6CE94A4B44F330B5D9832026DC5C6CE94A4B44F330B5D9BBD77CBF958416295CF7E1CE6BCCDC18FF8C07B68441048BD2AEB9CB7E57CB2C4B482FFC81B7AFB9DE27E1E3BD23C23A4453BD9ACE3262547EF835C3DAC4FD97F8461A14611DC9C27745132DED8E545C1D54C72F0469978520A9FB57DBA1EEA9BC3E660A909D838D718C397AA3B561A6F7901E0E82974856A78641048F96F5F09FA2A07893AAE77405F1D7E229D3C403AB6008AD1CA4C5608C92C99C666609606E48043203B5B05584D280B6975486BD3179F26495F07490912655918701015F200D444541544356434130303030317F4C12060904007F0007030102025305FE1FFFFFF75F25060104000500055F24060105000500055F37401FA423E03BA18714E98272477C86B77EFF4716DB490B427C34B212876CE063EA95CEF3BB6F8059A506B9DC194638278DDB81AE25E0592C43B9995B460486FE17");
			byte [] cvcaStData = HexString.toByteArray("7F218201B07F4E8201685F290100420D444553544356434130303030317F4982011D060A04007F000702020202038120A9FB57DBA1EEA9BC3E660A909D838D726E3BF623D52620282013481D1F6E537782207D5A0975FC2C3057EEF67530417AFFE7FB8055C126DC5C6CE94A4B44F330B5D9832026DC5C6CE94A4B44F330B5D9BBD77CBF958416295CF7E1CE6BCCDC18FF8C07B68441048BD2AEB9CB7E57CB2C4B482FFC81B7AFB9DE27E1E3BD23C23A4453BD9ACE3262547EF835C3DAC4FD97F8461A14611DC9C27745132DED8E545C1D54C72F0469978520A9FB57DBA1EEA9BC3E660A909D838D718C397AA3B561A6F7901E0E82974856A786410405AB6A1DDF4C611C1BB363A0BBC0E307EC1C03EA90CF4B7A51DC6798119D75173670D740FABA4E497EBBB01A20EA14D5A423FE7A43FB954A4A0173F2380364788701015F200D444553544356434130303030317F4C0E060904007F0007030102035301C35F25060104000500055F24060105000500055F37408C7551945DCF5B1BD8588859EACA6710B1CB690CEB28C3169F03B6CA76C75CF5A7FEA6DD16A60FCEFD1EB29A91C4471D6DC4161ECBFAE7ED4D1447C286A77F70");

			TlvDataObject cvcaIsTlv = ((ConstructedTlvDataObject)new TlvDataObjectContainer(cvcaIsData).getTagField(TR03110.TAG_7F21)).getTagField(TR03110.TAG_7F4E);
			TlvDataObject cvcaAtTlv = ((ConstructedTlvDataObject)new TlvDataObjectContainer(cvcaAtData).getTagField(TR03110.TAG_7F21)).getTagField(TR03110.TAG_7F4E);
			TlvDataObject cvcaStTlv = ((ConstructedTlvDataObject)new TlvDataObjectContainer(cvcaStData).getTagField(TR03110.TAG_7F21)).getTagField(TR03110.TAG_7F4E);
			
			//TA trustpoints
			TrustPointCardObject trustPointIs = new TrustPointCardObject(
					new TrustPointIdentifier(TerminalType.IS),
					new CardVerifiableCertificate((ConstructedTlvDataObject) cvcaIsTlv));
			mf.addChild(trustPointIs);

			TrustPointCardObject trustPointAt = new TrustPointCardObject(
					new TrustPointIdentifier(TerminalType.AT),
					new CardVerifiableCertificate((ConstructedTlvDataObject) cvcaAtTlv));
			mf.addChild(trustPointAt);

			TrustPointCardObject trustPointSt = new TrustPointCardObject(
					new TrustPointIdentifier(TerminalType.ST),
					new CardVerifiableCertificate((ConstructedTlvDataObject) cvcaStTlv));
			mf.addChild(trustPointSt);
			
			//Time store
			Calendar calendar = Calendar.getInstance();
			calendar.set(2014, 4, 5, 0, 0, 0);
			calendar.set(Calendar.MILLISECOND, 0);
			DateTimeCardObject curTime = new DateTimeCardObject(new DateTimeObjectIdentifier(), calendar.getTime());
			mf.addChild(curTime);
			

			//Aux data
			byte [] communityId = HexString.toByteArray("02761100000000");

			calendar.set(1996, Calendar.AUGUST,12,0,0,0);
			calendar.set(Calendar.MILLISECOND, 0);
			Date dateOfBirth= calendar.getTime();
			calendar.set(2023, Calendar.OCTOBER,31,0,0,0);
			calendar.set(Calendar.MILLISECOND, 0);
			Date validityDate = calendar.getTime();
			
			mf.addChild(new ByteDataAuxObject(new OidIdentifier(TaOid.id_CommunityID), communityId));
			mf.addChild(new DateAuxObject(new OidIdentifier(TaOid.id_DateOfBirth), dateOfBirth));
			mf.addChild(new DateAuxObject(new OidIdentifier(TaOid.id_DateOfExpiry), validityDate));
			
			//eID application
			DedicatedFile eIdAppl = new DedicatedFile(null, new DedicatedFileIdentifier(HexString.toByteArray("E8 07 04 00 7F 00 07 03 02")));
			mf.addChild(eIdAppl);
			
			//eID DG1
			CardFile eidDg1 = new ElementaryFile(
					new FileIdentifier(0x0101),
					new ShortFileIdentifier(0x01),
					HexString.toByteArray("61 04 13 02 54 50"),
					getAccessRightReadEidDg(1),
					emptySet,
					emptySet);
			eIdAppl.addChild(eidDg1);
			
			//eID DG2
			CardFile eidDg2 = new ElementaryFile(
					new FileIdentifier(0x0102),
					new ShortFileIdentifier(0x02),
					HexString.toByteArray("62 03 13 01 44"),
					getAccessRightReadEidDg(2),
					emptySet,
					emptySet);
			eIdAppl.addChild(eidDg2);
			
			//eID DG3
			CardFile eidDg3 = new ElementaryFile(
					new FileIdentifier(0x0103),
					new ShortFileIdentifier(0x03),
					HexString.toByteArray("63 0A 12 08 32 30 32 30 31 30 33 31"),
					getAccessRightReadEidDg(3),
					emptySet,
					emptySet);
			eIdAppl.addChild(eidDg3);
			
			//eID DG4
			CardFile eidDg4 = new ElementaryFile(
					new FileIdentifier(0x0104),
					new ShortFileIdentifier(0x04),
					HexString.toByteArray("64 07 0C 05 45 72 69 6B 61"),
					getAccessRightReadEidDg(4),
					emptySet,
					emptySet);
			eIdAppl.addChild(eidDg4);
			
			//eID DG5
			CardFile eidDg5 = new ElementaryFile(
					new FileIdentifier(0x0105),
					new ShortFileIdentifier(0x05),
					HexString.toByteArray("65 0C 0C 0A 4D 75 73 74 65 72 6D 61 6E 6E"),
					getAccessRightReadEidDg(5),
					emptySet,
					emptySet);
			eIdAppl.addChild(eidDg5);
			
			//eID DG6
			CardFile eidDg6 = new ElementaryFile(
					new FileIdentifier(0x0106),
					new ShortFileIdentifier(0x06),
					HexString.toByteArray("66 11 0C 0F 4C 61 64 79 20 43 6F 6E 66 6F 72 6D 69 74 79"),
					getAccessRightReadEidDg(6),
					emptySet,
					emptySet);
			eIdAppl.addChild(eidDg6);
			
			//eID DG7
			CardFile eidDg7 = new ElementaryFile(
					new FileIdentifier(0x0107),
					new ShortFileIdentifier(0x07),
					HexString.toByteArray("67 02 0C 00"),
					getAccessRightReadEidDg(7),
					emptySet,
					emptySet);
			eIdAppl.addChild(eidDg7);
			
			//eID DG8
			CardFile eidDg8 = new ElementaryFile(
					new FileIdentifier(0x0108),
					new ShortFileIdentifier(0x08),
					HexString.toByteArray("68 0A 12 08 31 39 36 34 30 38 31 32"),
					getAccessRightReadEidDg(8),
					emptySet,
					emptySet);
			eIdAppl.addChild(eidDg8);
			
			//eID DG9
			CardFile eidDg9 = new ElementaryFile(
					new FileIdentifier(0x0109),
					new ShortFileIdentifier(0x09),
					HexString.toByteArray("69 1B 30 19 AB 08 0C 06 42 65 72 6C 69 6E AC 08 0C 06 42 65 72 6C 69 6E AD 03 13 01 44"),
					getAccessRightReadEidDg(9),
					emptySet,
					emptySet);
			eIdAppl.addChild(eidDg9);
			
			//eID DG10
			CardFile eidDg10 = new ElementaryFile(
					new FileIdentifier(0x010A),
					new ShortFileIdentifier(0x0A),
					HexString.toByteArray("6A 03 13 01 44"),
					getAccessRightReadEidDg(10),
					emptySet,
					emptySet);
			eIdAppl.addChild(eidDg10);
			
			//eID DG11
			CardFile eidDg11 = new ElementaryFile(
					new FileIdentifier(0x010B),
					new ShortFileIdentifier(0x0B),
					HexString.toByteArray("6B 03 13 01 46"),
					getAccessRightReadEidDg(11),
					emptySet,
					emptySet);
			eIdAppl.addChild(eidDg11);
			
			//eID DG12
			CardFile eidDg12 = new ElementaryFile(
					new FileIdentifier(0x010C),
					new ShortFileIdentifier(0x0C),
					HexString.toByteArray("6C 5E 31 5C 30 2C 06 07 2A 86 48 CE 3D 01 01 02 21 00 A9 FB 57 DB A1 EE A9 BC 3E 66 0A 90 9D 83 8D 72 6E 3B F6 23 D5 26 20 28 20 13 48 1D 1F 6E 53 77 30 2C 06 07 2A 86 48 CE 3D 01 01 02 21 00 A9 FB 57 DB A1 EE A9 BC 3E 66 0A 90 9D 83 8D 72 6E 3B F6 23 D5 26 20 28 20 13 48 1D 1F 6E 53 77"),
					getAccessRightReadEidDg(12),
					emptySet,
					emptySet);
			eIdAppl.addChild(eidDg12);
			
			//eID DG13
			CardFile eidDg13 = new ElementaryFile(
					new FileIdentifier(0x010D),
					new ShortFileIdentifier(0x0D),
					HexString.toByteArray("6D 09 0C 07 4D 75 65 6C 6C 65 72"),
					getAccessRightReadEidDg(13),
					emptySet,
					emptySet);
			eIdAppl.addChild(eidDg13);
			
			//eID DG17
			CardFile eidDg17 = new ElementaryFile(
					new FileIdentifier(0x0111),
					new ShortFileIdentifier(0x11),
					HexString.toByteArray("71 2E 30 2C AA 11 0C 0F 48 65 69 64 65 73 74 72 61 73 73 65 20 31 37 AB 08 0C 06 42 65 72 6C 69 6E AC 08 0C 06 42 65 72 6C 69 6E AD 03 13 01 44"),
					getAccessRightReadEidDg(17),
					getAccessRightUpdateEidDg(17),
					emptySet);
			eIdAppl.addChild(eidDg17);
			
			//eID DG18
			CardFile eidDg18 = new ElementaryFile(
					new FileIdentifier(0x0112),
					new ShortFileIdentifier(0x12),
					HexString.toByteArray("72 09 04 07 02 76 11 00 00 00 00"),
					getAccessRightReadEidDg(18),
					getAccessRightUpdateEidDg(18),
					emptySet);
			eIdAppl.addChild(eidDg18);
			
			//eID DG19
			CardFile eidDg19 = new ElementaryFile(
					new FileIdentifier(0x0113),
					new ShortFileIdentifier(0x13),
					HexString.toByteArray("73 0E A1 0C 0C 0A 52 65 73 50 65 72 6D 69 74 31"),
					getAccessRightReadEidDg(19),
					getAccessRightUpdateEidDg(19),
					emptySet);
			eIdAppl.addChild(eidDg19);
			
			//eID DG20
			CardFile eidDg20 = new ElementaryFile(
					new FileIdentifier(0x0114),
					new ShortFileIdentifier(0x14),
					HexString.toByteArray("74 0E A1 0C 0C 0A 52 65 73 50 65 72 6D 69 74 32"),
					getAccessRightReadEidDg(20),
					getAccessRightUpdateEidDg(20),
					emptySet);
			eIdAppl.addChild(eidDg20);
			
			//eID DG21
			CardFile eidDg21 = new ElementaryFile(
					new FileIdentifier(0x0115),
					new ShortFileIdentifier(0x15),
					HexString.toByteArray("75 5E 31 5C 30 2C 06 07 2A 86 48 CE 3D 01 01 02 21 00 A9 FB 57 DB A1 EE A9 BC 3E 66 0A 90 9D 83 8D 72 6E 3B F6 23 D5 26 20 28 20 13 48 1D 1F 6E 53 77 30 2C 06 07 2A 86 48 CE 3D 01 01 02 21 00 A9 FB 57 DB A1 EE A9 BC 3E 66 0A 90 9D 83 8D 72 6E 3B F6 23 D5 26 20 28 20 13 48 1D 1F 6E 53 77"),
					getAccessRightReadEidDg(21),
					getAccessRightUpdateEidDg(21),
					emptySet);
			eIdAppl.addChild(eidDg21);
			
			//ePass application
			DedicatedFile ePassAppl = new DedicatedFile(null, new DedicatedFileIdentifier(HexString.toByteArray("A0 00 00 02 47 10 01")));
			mf.addChild(ePassAppl);
			
			//ePass DG1
			CardFile epassDg1 = new ElementaryFile(
					new FileIdentifier(0x0101),
					new ShortFileIdentifier(0x01),
					HexString.toByteArray("615D5F1F5A5450443C3C543232303030313239333C3C3C3C3C3C3C3C3C3C3C3C3C3C3C363430383132353C32303130333135443C3C3C3C3C3C3C3C3C3C3C3C3C344D55535445524D414E4E3C3C4552494B413C3C3C3C3C3C3C3C3C3C3C3C3C"),
					paceSet,
					emptySet,
					emptySet);
			ePassAppl.addChild(epassDg1);
			
			//ePass DG2
			CardFile epassDg2 = new ElementaryFile(
					new FileIdentifier(0x0102),
					new ShortFileIdentifier(0x02),
					HexString.toByteArray("75822E7B7F61822E760201017F60822E6EA10E81010282010087020101880200085F2E822E59464143003031300000002E59000100002E4B000000000000000000000000000000000101015101C10000000000000000000C6A5020200D0A870A00000014667479706A703220000000006A7032200000002D6A7032680000001669686472000002130000019D0003070700000000000F636F6C7201000000000010000000006A703263FF4FFF51002F00000000019D0000021300000000000000000000019D0000021300000000000000000003070101070101070101FF64002E00014D617572657220456C656374726F6E6963732C20696D67696F2E646C6C20332E32302028373037303929FF52000C00000001010504040001FF5C00134040484850484850484850484850484850FF5D0014014040484850484850484850484850484850FF5D0014024040484850484850484850484850484850FF90000A000000002D220001FF93CFB2D120E5644D1959E03A641A07495EA0E63B40280EC711CC23309EEAC03C1A983C462F6296125F9A0B6DD22DC6C0E1EB92DB02003B48266815CB7E8DA868C815F48BAA852E4CC50A0A0F407845768DB44F10FC1FA318B8B14A29B74A780129DC61773B4BA6D985668050038A2FF974BFBA16FA5A7F83AB4E97BAD11FF3592A6F90E0438D02822E5AAFF0D87AE55FB8823BB04C973F079EEECB784240160FC2A7CB3EE610DD72A278BA645A825C9B2D38D8AFAF2365735801D2D1DA9A833676ECF8A8F0D90DA3C9F0D8DE16ED2A961A19B5BA7FC7D3AA002498AE86E8D654A578A0E2EA40E61F8C817AB6AF045C5CFB93FF1AAF483E39D5ACA226B6354A005894580BA3F08EC46FBA4EE9436370715A47DA990012854C8EB78125E0BB2A068161768552B84329FCE66CB3146A72539528FA5FB2EA9A89D2CC7DD5D31EEB919EFDEB0964BE4A066043F54E6ADEF9D740D19EFF817879828610CF3FB4AAFC8561687D702103F2DAD987868875B6FC561E66CB66E95BDAD30F0831389B6C3FA31FCFA74B00817FA3D4EA79664F7A9B79A86248C200F378306D88F66D0B0C1D6C20AC918564E7E6E6CF4148F96D1B35B2C5238C1615C5C19B7AAFE6B4C13372C540ABACE968ED3E90ABC619E5D150AA2C3653227A0C7B4107A39C97E95D796936E43E510AACD2C7FD3931500CAD567F0F7127DF4B7EC7DC3374A43BB3C0146B97F76B1F8FFC488467762F7DA8DCDC0FABFBF6067FE6960C828AE950C7DB5EC7DB5347D744005E527C10707F08AB966E0DCBE93E4CC43FDF0BD9D8575387FBFA5E4B400AE204645B6B1A2B85B1524DB2147D49273FB173840BFDF7FBF98E8E347B8CC5EACDFD42D62C7BD3DBF91BAC3158A3E7A80C4A08192E628D6338E8BB51F3A5EE9E72266958053D1BDCF91C05F3C704F24F5804A3FB2009EF8B9CE858372A6FD4A0862B14C89C171C2DDE40FCD715F11F5B487C576380651E0103F62400F5FF4AA50734AE37FF82640B6796E6193EF1712EC3CFCD70DCC1BB06FC4E4DDD00837F289F192C8642018ED3A24E0798B76FB55AE89A4FFB8F3E16122107FBE8F3474654BE0650CE6BBAB0AA3E2C41793D5A6005D5C8F605237D6B1572359DCB05681BEE10C5C1FF6EB08FA6DC023AFD55515303B93518CCAC188CA5BCEC06868F305C2EC1C30F83AF3D6980124C754E2BEB6FD38E1B297579E5DB13BF39ECFD3146A9F0ED70E3E461DCBACFBBFEE824AC3771A994B570C85B9F2FEE1D0E2FF4F4E4501768AAFD1B7F9406E4019D4ECD9E14FF06C1E1FB5A1A2518D7F1277992B964D447E10E53D9E4D1F55157246E17DDAEDFCF910D797195A61A5BB804A98064D7B25ACD94872DD9E0D1973DB160F31833310AEF952494D4D84CEAF05FB47DC99FA1224F6B2185593C75EADB73EBFC20CA5F11AD32DB3AB8BC376CB833434A1840B1B4725805443A348E76A3101B12A7C3E5B443E5ADC1F152809403B801A60E70DAFF7801F68D1FAFB9077512B9B84C4862343C07DBE004F5AB5BCBEF2CE4C97FFA85904618B3868040AE66ECCDA537C3A4AF8AE42E9C8474E4EE807452930EF43D07D892027D9B253A2D9DC5662CFBFBEE9642D0565A8AE44151C74BCC03A001298DA421206D79E16544D44A123B18415DFF27AED7CAF00A0DCD208BC55854518ECF729E06F02E0ABEBE296D2E8398A2F118BE7C5B67B177E6F73641EC3D455C6F799060CADE0364024EE136DC0CE09E4A35BDA1C1276391242B98A5725254BB60DF951392358CCCAE5CE5FF00DC1DFACE0D22276150CF63E24395900FC74E5096BA674413DE6893E023E082FE53FDC4D2E31F41B8661DD0161DE5026D42DF76602C4C6038ECC3E5B4C3E3A1C1F1D4407B56F4E5EE57976181A248A6425BB35ABB4CC0475DFA195C3AEA116977E16B29A3CC1AED33A604647E614ACB058CFE969B96D89421029C596BFF534C8A4306AFADBB420E34C955F635327ADDB56A374756B9807793C3AE99CB7DA2C5C8B7D6DBE6DD410B1FE60E0D436B65CBBCF625DF5767C31FF023BDCCC4A04B4F9002F01F7045E95958C6EA6A718A5BEF03412B39527220C70B25EE2B196B713146E3A6F623D51735B8D7BBA8AFA4740268C0445977073E6D6A7803460CFBD440EEC29164F630408861137F7917D549E2C5173C136AFE6142C5E8454C64D4FA5F8E6E0DB44F215A584137149B06FD9E4FFE5D66EF4F83A311C614ACCD2D41EEB42FCFAF7063E8EBAC7D1D52F301A63DD7FF8603E6535FD696510D9F959800DA9B5DB22EA45D6EC23DD70140E08F244FB490A9D9739C28B8A27DFBEB4AF8FA53FE1A4A098FAA4F52DE85A3EC4EA2FD1BFF612CA3A0678241A2C6DA9316F65B1FD61C9275CEA04D6DEA619FD3491F95ABFBFF61F814BFC17C5FC93312835C0DF13DD0DFB0E3DE99BB227389B874AABFB460E03C9E23113F26A5C05A11956D9FB8425289B6A095C415570E7767E755B3DC1DFD0B7F9EEC61E34CF2D2DC819A25E18590833C3552BB803C4F3C868B58604F85020AFC12FB3AD872E6C5E0D72FFBDFCED6A2A4E09CE575A9C64DFE00DC4FCDE7B45B5E0B6E8850D757406743E0746891314F29203B3C73B065075032EEA7DC23D538144D830B5716452822DE825CEDB0B2A6EF6E22E47F4841CE415A6177EF97595AF970D60547AF60B9E0FCCAFEA0F95935674E0AAB30C41F47A6227C42CB6C71C6117A77971EF4CC302CDCF0DDAA812B3D57AE21FAB5F0B24E54C2006D89D4FDC563C99AC40B39D36A2A385D11946341270500D65273C4DFEB8EC260037D15469DA5BE9A88798FC40ED19C4525BD4EC8923FE215E6DD58B591FEC6EE8FAD886D70667F5039F19E5898362CCB4DBA8BB189F1B05BDAE160B063D324C14E30D0A8317222080A15090C6B36150955778AD35C8E6AE5FDC0C4E588C4AF59627F89F14964715D2D8728A2BCC1A610D09C9B8DDB0557714CA4530EEC4D9E9E11D940CD6A087CA52A9415961F298523B54666C5E00C53510CC1813119E4C00652694AD50391DAA25DEAC7E7B2C4830893C67CA26A13837FF92F657EFD7CB11C739397715B2DA41ED72FC800D7A1806CC9632E0BD9592E545996F22D56E4E31757480027BF8D3B082BFEAA7CD5B82D4B277FC23635A910D3D115EF124CAF307A034D57A826F6AED5C9314491FF7697498629A1265BD9085BD6B89BA474FB76285E0BF69F05E6BA2065DF38309947F9F34016C2DF2DEBB80F7D8CBE96A7A8ACCBB4EEB0AA5C8741A9E5CDDBE6DAEB46749D30F40B897A49AE978E099B406794CA1E05ECE04E34AFB62EEC351548642A88BD4F8649C5FA085E0AD7D2BA99F2BF464E4958704FE50D1BB9CC6BBBE59A34D83162A1B4A85D709D648E8E691AD9A5F8A319CE36059372CE16BC5AA2825449928F818F51DB6EE2B1A94FBDE6E380F8EF14AAB1FE27C092F20B66DF0423AD0652F9946C0FF7C5B459D847D2F53FBAE06359222A7E63456F848E8349751D3C3C212428227134365594CE35BAC213E6D6362030BA81CC782E5A255EC6073E0791D4B8B193782C09BA7B1021907A9DB6CC8191C9DE68F740D4823019115C8ECEDEC8B22A8F86B05A328F1789BBB4C9D0EE5816871F07B90D1B76395AB8CB262A96ED0E371C7806F6C4B58E11095D03F8C5EE9FB0A79FF1F3ABE13499809B4C191D7EF799B02D5E60E064D2A0091027357D3DAB096A50AA1B081816EA1F626AB0CE9A5C93CA459A5D1910E289C1974AB221CC2C33FF3AE1C3244495220D40954E8A03905FE48D630D6919AD8EBF4D91DAAAC33268DAEF4955475E33F9E5188415B0DA2E7626711B517E7D7AA61BF9244288C256E9536B272D7B2D1BF6CA9ECAB93926D42D9BBCED11D322E37A0E183522F8526CA28AD6E897C3E2DA30F874483B56DB686322C80D15FE6F85451434FA0127995AEEFE098FDBDC596F283C51D2CDD0BA1BAFB89599FF72A388759709012483B0771F895EEB34894A04B213D319A2AB3E6139502D91AA775ACF4DBE952D336752F1AB9A4CB0B668E0D190BDBA2D43283BF80489A322D64A5B9B2EDC5FA2C282B585CCF7318A4B49DC76A8CBC0EFAB4D559ED2C0C5281F6C6E123DB8536334936EE78E4F459C457943B655D76537F47D27D1B60D3CF55B4254184F1298E6ACDFE4433B196352967D48F0FF41280838189FCCAE092101E8417CCA150A479E0BCFAED0841EB501D74DB8653A774983A1AB9F0DC8D592DACE3C1C0AC0EBD4AA19520E7E3B8AE753594F17FF50A5784B379AB412DA59E80D70B0BC37728FFBE23AE6B9AA30B20EDB232C9C9423486A4411F1CB857EE8ED4BB5458C8194553651967B18FF073F097B30CAE5089329CED413A4C3E2DA90F875383DA680A8E1D3442DB88BEFCD7BE6E47BCBCD6D080AB07D13BC54484B5B265F2FEC0D42DCFF0C4F7B6C76EB394B96E7A8F0131D158833C8FF40E9BF81EDFF30599AC17921BA8833FE0BB1669C5E7EA7AF787A0B199E1ED766DD2C84E240DF2D704D18C91D2BB3543A4572EC4CCB8ECC04AA3B12A718C63969DF4F5EB2B7D521D00FFA9AF156304517A8A352D006820E9BC77A89764C56C18AED59E74127A160A8B006537A6F94B95C079F7BAA64C5A1AB7941425441BBE12FA88D9E1DB95E04766F01B64C620D2675601AED62DA3C4EC887D297D84607F0C16BCB704CB04A16069701635CC413E21EEC078885004B709205DE78DFEFB92E9060130DF82FF192A875B4C4C80943C76E77AA6D26F2A64557FEEE52DBB95970BA27CBF523AF26A5E269CA57302D9AE5FBFA73AA726C39085CB88898DA7A7E614C4D582626448547EC0FE92A7BBB408CC574FE8ED276B25D157B7967965120D5BE38221766B1E7C0BE8397ACF75AB6968ABBA469C58512952D8C075632CC83C2E7E8FA027E8A2E3F2FAF1FCAC73F47A7FBE35CF6B4615FDC538C6498A822E080D9B601ADC7C95EF2FD2B1D9194D971D8B66C36FB3A95CA92FE7B41DC9E5A9B749D2E8960D70CFC6F847742E8FD8191E94320D508673C7E1856183447D611CEEEF7C527A4B4EFCAE5A3FF6168A5D084E7B5EB50E5FADF6EA13F754CBE9AC4F821C6765D5B6561E97421C86442768F4B7BAC3F2AD2BC626F07D0767F6A265D5E0B87554BFAD3BF1BF306B99424DD89D57AE9E72D6C952E47B8AD18A40F00CDEEB5AECB37D43F840615F5C26B55AF6D01828EBEDA35D68DECF366898282D85A387FEA629D8715834DED881A487444970A593C35F8FAECB9684589F2E6CEB77AC5C7EB604D4C1A8AAE4EBFC3197B05FBF57BB57C4FC667325A3C8E4FD0F85CF932579BB687A0A31AB59C7BA255A55F6CF77072340912C37DB6ABF7E64C2AEDCB0FE001B94E4AE67609239A25DC5B901D8D9F05263BF5BB5F36502216C1A1E571323E13EAE8955CA7C40F88BF0B36AD1789A2434A0533A2385176F78B7FE1C6E4EED9C8535951826A6C731F5C924BE2878F5CD6789BEF97F3A498157A24922C302D6E171A00BAA4D2CCB16585EBBEFE82D02B2A686CF35818032D82E02B675B2DE1FB37469D69523140E44947B94BF67E65BAB2475E3EDB98C88B2CCE7F03EC2615909FFAD97B299F9DF9931D2FD238E75255CB3E5DD6C3406A20EDF0A75D53D968C9F0B42D1A54BB89EF422613992424CC433C7662543E382022E46F358C210380046E2B784B1D69732A8FE3A9973AD0BDB7AC529A22CC61B15EA3DD427D87EA5D6AFE08DA9616F3739D3E037EA9568AAE9FF5AE3B4E48771D41455F68011DEF8ED23322B84C8A8AD38EE81BDC1388B3A6B79092198604F4F8C7CEEE6D3B862C57CF15C83F0D9DFCA78AA2D7A6E7304BC983FF3EE3B1844019CCEBB1B6973B6BF502D16D928578F1336287417CD11172700BD26D95DE8EEBA47EDCAD2C1DBD220DD00F848132235AD067518812540B50F5B807810EDD202A93A62BC605BAD487B1082B2042AEB43C9F2457613EDEEA0241FECB0A64E5235C915757496CF842C604EFD56B4B898D371E2E6330F4D711BDE9F38999775D0D09C89C5BC7215CD3429174AB947C4F7EC576A86B929E1E7ECD1116FEF14325B6BF4C01FC5DE9A4415FB54847548B472FB33E9A33088F4484FCABD6EB26D929A352E9B4749E785D707F848E1BD47032E5AEF3F5C57D0E074E7C858AF3FBD5AD2F96CF9835F5CD2023DC9AB7574BA1A2578369CF95C05982D3D45DD5B91EFDAC15A4BCBA77D26122235B1B65E28F4633FD75EBDB7448048D734BEE96C0C5671FA6B56294127CAF8296DC94867081E03A884762ECC701CAE249C4D4EC6EEC7320B603550D41095C96AC9A3C2F8BDFB382E710546ACFE933D985DFEC6947CBB6413F0D5290DC0256739F1F50971DD86C9E6015197000A0D92979AD3098625CF4D777027A2DADFD3E51E13A16D8B7213E2CBBDE8C2ECD1BE923E4C6979BF6E821F3380D6B3E17A22B0144D56C42B18231EBBB8815E65D4F680614F3EAFBF1521311C3600170AF3538FBE84D0B3987F40BF68C7EFB4FCAF15ABC8A04F3C7CA528CFF11FDB3040154E07528A5980CBC6724F934132D07B2806A0D739CE2CFC777306AECCCAC217D583FFF5219BA28A9D04BD29556B4FF0F0ABAF25A0C75B3BD19CD9E18FEEF5647E91427B42ACEDB1ABB94D94984B93137C5BFCF29FC410A50F43D250B1D0A8C2758910F0872AFA1122C03BB280D4BB0660D7589FD5CCAF16934E35EC52D0DA5C05B55A9B081F24CF0229350CCF28ABCD7434D7F8CD368AA60FCAA61C250D62B94933F3F603E5F6B706CF3743E59A19CED30CC3DDE0789B8A886CCEACA61D38A3698B796D17A722A7130D7428D928DA39235B7DA4946346050096D13FE2E4D6BDA725BA3CA3899936FD62E8E3E2675EB97AD677920E325825AB72E94272BF0CF3329B907F98E74F1B301D25603511A9DA82F9B60E790722FE9DD868B5E9F0279F968E47873133CEFDEF2D016D04111D2B55E0051C76543B611BCDB6083E363E1B09723F7BD4F3A2D7891A887E1812D5CD88DA4FF48D3F67F20EEB87E3FDDA69504D8F501744906C5E7F4BEC61E443ECB53349043CC9B929AB2144394322961DC8F5919DE0E7F04FE7669490203902337FCC1D52617D8465BBA4571AE093BE2F6A326415330698B874B7161189AD7AEB203627269337ABCED695BCFC0EC862DD0F7F081B9BC99951D182BF9CF86D8381274BA0364B86E9B18E1061FC8E51D1391F8F2012D8E6F007A841BB351DA32A57D51FAE61FEB913A707192E972B42AF840E5FD3E6E487B719AE9AD96D3BBD11C57D8B9F5E5896180C724E5D016F3554FD4CFCA53EE3DEB9D725A32C0776DABFB12A8F33D30A66F4F8FF8257FFCC45071A8FF6048CF62B67CD703E591C47D9E9B431B8ADB594C63C203707D9C5A4A20E46D85AEE1D292A8FBF3D85F219A074D385FE69C95E7752907D8E06F8C4E2E06161079C3438DBB54DF30FC6728569D471660B59EE0BA54D99994D50648FEE49BB0B2F3009B16F69C34D3BA9CB11AB0B4866AADD838A559BF0AED7ACF01FAC2EC44E8CDBA08585E0BB53B41CB1038D669D9DF7AD652141B14DC0F5286D7B83F0FDDAC0557C3DDB1570E3BB4F518D74D22FA7556EBB608337A00A85FA3AEF7E15075DA3C7B340D2299F6C33FA867FBCB3B4FBA0140D6D2A7966C7AE81BF7D03E2A6277BF46DC9EF3045474A3EBCE28920A00834011E6DEC9479135CC108D4BE0C48911AC8C5F4070D11A1F277E1476D5BF5A72A10A243CB57E18D899895EF966C610805B0C4755CE72F307461AFD92DEEB653A5BA6B52DF2185BCA4B18B1A88D4A5EFF1464957BA9BF0B6781C2FC5B6F62BE060A7F77E64D81229AD6DF25DF7EC91383CC6DB1788341B6A3AF873B6D5D38DA224D6B320562F9CDDB2CE9763B8682A7BE35E432A143A33231F17204F6F51538CEE93C0771E9B45DAA386B1EAE5BFA81F68AC3E46BED7BDF5D859F59605E753EBEEA805C5AD26C48BBAC34BDDDEEEEAB3CDB6E63D9026B740E5E5EAD3181EE9696464E19C88261173CEB13C54B85C9C8C05DD2B9EED755CD775538E8ACB5815C3299F4DD256D6E92AC762ABBA14132D9B87A2DFE1AD4F6076282BF4367669769B89DAC9E4A6DAE84E7FC30E47442107587E900A8FA8DFAFF4C7BC67E1812B6DA263AE4F43F184BE2B573C2CE1028ADB76EEC1BEC38B1EB4E9D25A4CEB5F34725F0B00A505728A023A9CE947FB42DF7DA6BB818486C868EDCFBC06711E72F0AD0848B008B6726C7E4A58BCBE2B6F1B3F1B90486CA193948B41F4312D6969868F9F966435DAC3963BC22A554B15DFA7112307A9513E2ACE53956A01023957402A2DBFF6E9BDCF20974D5EC0FB6A82292087C0C11868CB0E437244070C74D58519BC7D6BFB35D011978709FC1E4BA7631E7A22BAAF780C62A6716171A8B0D13F1E880282E1E86A354FDD51787001755BA70C7DE6D7A47942A1A3D8585D6FF83AD4A2613DC204EE6E8690ED1CBA49ACA6424F7F2102D80A9BCA28A7F158510DA4A38EDA9F67E14C90157955D06833C4BE00563A34E2E42EC0F68AA5DDCE928A82F01C981AC235447C7675E120549EFE48B87951B259940ED8DA73F54B0780FA43F143B37F2B1E1F83A76CEC3DA80F308A29CBF6210BD867F41018A7A85D1C0BAA4B00221ECEAC01345AE4E0A567697022AD6B9BBD44BD8B3510A5DB1D64896986849AAB5D4E4E964AC6235D53192381D8CFF2F22FAB51AD1D056A202AA548BB17177438FDB93490948D817BFE138915914A5426E5EEB7D0D4F6CD70FF2AF5DB31626D42D2AA1F3E9766404E84EF4065E3F38F0E12DE978474B4402B13EC20C08FC7565C353F8C5DE8B8B2A0C01A3CBBF409603F9CC937B9BACEBCD803E2635750F93D6F474E939E6218406286264CCCADB5B7942F07179AC6CE87D8F9BAF28A230E1EE975461EDBB076840EBE990BF504849363AD036346A4E2163699B238661C9A919F3457873D92173FAF490F3D63D0C51F05CAEEE8A403F7606A70725F024D4422EA48EBF32CB94EE18038AF683E08B4411201C4B79944ED66B24CF05EBE6B6C0420446EAB1C16AB0D10444DE3F0BC30FB2049692758102FA2F9CB1E034309EACEE2616FD45702FF9430C59201FFA4A250247ECD748A13141CBA9AD26BA1C6F1A66E563F2346D18A8A505EBF73A8CCA0D96A5AC67F3F97D0C7E6EA37E6F451F97BFBFE5A57E57DC7F17626F75E7E2DAFF47A37FC4E7F1ABE3F8F705F06B17C1A47E3D0DFE36DF8940F82D103A2432EED2F274256B29A2388476F30A623AF1D12AF9E125B6D5748E7426BE897A6CFB3895997E392D94AD52B896FEC91F57D7AD2F1B6B3E849D905C7F921698201C0B06CFA1CC766F72F39730BAD96CB8D5454E8D9112DB1FAD45F7DA5D94410D7AA8DBD493C56D87BB1983E17A131855F3681E2363BAF2F9F48A786964F092721B33F18BB4F5581CE957AF9B906A449FCEE5540A2300F05F744D06BC9138FDF7BCEF6839BF658CFA83DDDC92F815624D10BA914317A8BFFA5BDD2F649BF117239B00C3260515FE95B430F7FD99BAE4364233E354B062922035B26DF317420033E01C55E88B25D3ACA7854D149B4A2D858492F1FA4E9502017870E13BDF89C0EADB43117ACE5843CA6CD16E04196F06EC52529BDD6DB3DC46C1B8422F8B4325F164C7CCF7ABF89F105DE24EBA22B412085E5F0381CBA0FC2F5AD46E4EB1F76D3F267C57103FEEB65C363D69AD4DF01783B5798563EE7F52EE33EC9607425A1C2668F036380354D4BBC3A3802D12F5056721B1560A5722E01F1149D853FA859FCB3F6F1B951A5F9BCBA6E9914A8DE26971003A3FAF96D7D19BF8833F2A8DD0FE5F6654212643260EA7A2A4CBE70D31760DB4763EE4AEC8635A19795D55F44B9C1F284A9ABDA113EFAEBE0E1B8A2477C53816B4D6F188CADA54CF82720D4E29B6ED8DD8B39A7C389AD36E0449099F420A8EC31D895F6406BCE87167E8EBF57F81534610587235C9B7E26D49694CAB7E66053A67F109F1CD20EB4B3C386EE8C2ABD551939083C5A402256C79BD1AE9750983BA65F064834F10FD149981553806241837CF2850E47F0DA189628D64D87F16C4C9751503F7BF0E046664C7701900AE5925F49FA60A61E046AA9C4665AFDF379A9EBF07D06A2A96746A3CAD4849EF4103E0A2CCE7D4E78B772227728B94ACF1792685AA550890750D8E6E9BE59D4A6AA07A463218AE0A4B6BDF87C31A705601E9CD98D064A135F38C9A504FCF12AB8081C47F730E03209C26FF5A13C8A50F8C4F108E38B0FB50317B378DC6F06D94B77D335A6B9FFC01237F3097A9A1EEF1ABF10D256308DEFDCA51E06E590F78C67F78D1E8E0C6675B7DA45BCE6DA46324928A75A73608D7CCBD87716FEC99BED6588BE69DDBF970B22FE7BE5C7E9B3B2453C742E8EC9531F8A2956D3D6793E7C3F33FADD997A4126F916469FAA2F62B4F292AE7726181061E1769E4E7F860B532E09DEB8EAB2CC5DFDB2A1659B86A2FD909EAF0F7924DA1875EFF1025E27B10E275EBB2B4E15BEA9C6A8A9728DAE2DC7C43159D0BDCE615851B0AD9F5D9DC5A725CB8780D9F5594AEB39D815CA1873EEC8C9A4F76FA9B115E71DB2EAC5E49755AAE981F1DA0465CB74E0AE3DB8441F1292CC3F4045438278A96EF9F4B8B3C4CB3C508D4F76FAF05B7005827B9894C88CF7934BBE87FF2632CB7DF146C0F2400A916DAF8E543F97E9FE2444EEBCB6E4854F4D85D00BAC02B06236F778D41EE7E5DBC76AA8491699E66131CED14A81B71D5003DF885E0639CA94B9953CE1EC9C68C9B6CD220830D9778F958E5E33B8A5708CA2609F1F6F38093AF5107377C3C9B27603DD79282712A6D2F6BA2525B9C78FD6A292025DA60E860D1AF0E8A643CED299F51BCB0A93B1D2071663339A531F08F77268BE3189954B9AAB55B1D0627FB9E9D1EC90A6F3FC4666BC1CED66F21AF59D000F81DC98DE516074438826C1373851BC2B768FF74AC11BA5D883C014B6098C819F3359EB6CD9B75E05C1373D914A62B9BC8996264703428D92EC29947AA42AF326F9E628D93C98526840FB6893E53654790EE4677EC0907895ED4D3DF46F6EE36CED3A9438324BA943A73D6E23F43D6A9A7DDEA46E04C0515BCF6B19A607A2C34845C6B5F1B4AB456DFDE9C11505692BAF93E994EB3AE7C0D31BB27A7ADD9456EF775D34939549E046AA16C16FB824B5A3982CB6F76A59DC8C9A8695035EE8FDE26F9137E63F5390FE1FF7F4C9DE58EE9DB42E0D6D35085837220276D36B6CBCE77955768F97C2BAEC403F1F548CED4A8805AD89E6A2455A414337E413B0BD1A47867AC02F389A0C0F714C1B0262F263F57792D8B02D24CDB87F13AFEBCB139F1D0A0D00E60367A47F7301BE620EF0989C56AF78A90A62E1E66154FA05E43234967AE8B117B194CB2876A36A9779F5BEE319AA0C62BB4ABE169B591C62D0FEBCBB32D40B8EFD36D128F9E062CEA37DA595B7D9145B18CE8092359CEC1520F1AE8FE74A4F1D33D6F8437BE40A158CC9D2535E3C30CD63B9E3C9A83D0E4E7DE152865E19BDA691E4D78C2979230BFDB7A22E51A6DB5E8367422B8F77C3C9479E5B93B6D6EC2FEA456116B8E74F33DEEA7A5D0E04CFEB0BAFCA0EA6B2CE8F2403714929981A98AA997424B2B149106EF6791D598ACD2ED30C25926FB80E5E92EB6F408511CB8D618049D02CFD48FD09BC63DD1D0A69B5B67335DEF65F09BA62A296E0816AA58E7643F8551DDEFAE91649A4E639E25971EF8718352FCC9EE7EAE2139CF83697E33CBDBC82B60090C71FD2AF1C0BB1A4E050DC994F24D16C91F24846FBEEF2CB0A45E445A1D6857787F48BE4948F0B89E48FF4D0806EBC6EC08899B001EB619C147CE5F39867BD050C025E533B132C99706CE9EEBD313A3300A93ACCFDB36F7B90D59E9E915663F238DB0259844D1E1813939E644923FFDB4F0CC141151B41CD5F671D166A55AF5E14B02244DD2E46FDC3FF36D8A5D967F2667595B144E6C93B2096D6C37847CB03AC6F3586F9B7FD46EBDEAD49A17279C6BC390852E80AED3E90A758A5060B568461B38D6C2607AC87D69E1EAA85785963DF7DF517E517360015E820A5B4F50987E14DC0B31E28F422CFEB694D4311948514F2DFEAAE108A87109850133EFE7A1E018E89D125186F8B0BA24DAE1EC0E23DF60F856CA97F376EE690E8B5E36DBF4D7019DE53FC21EC8644D0C1182854A589A7BFBB1B35ABDF46461F1CCE1FB5DD29E7EBDA9476665D0429C1D72EF21603BF77919A0D151F869CE4FA4012414D67DB2547B75755742421A021576BE5FA6059AEFC6237167F6BEB9CFCD7791BDF4EB5729E9CECE258397FBCF55AB29F7CD11141C0A47332113B4E846DD538FB90F053BEA2335BB9E0CAC6B7618A52DA4F9F0DF277846CF3D89B0733A37A3675DFF021B8413D79B5AE2FA143EBCB5A0E38CF08A0F17889B6848470DBF1AE02BAB427E5E03812513EED7A99BCB92DB9615CA53648178A7336DE32448878E1ED4C3950BE5B105C3D1DDCB5EC249926FA80E699C38B6F51CD97722AA175E303DC2611D77F448952A778AC716B4C8E1137C52F08A4DDFB80F6C8439A4F1BD1EE5440368AFFB082A2EC287C0289BDBD22544A6EA2B02D7BA2579C44586E3BA28D3B34EE272C2BF7BD10139FDFDE786443163E619B8205A26B2E4A3F8E448C923CC4F38AF06EA9CE0E47468FF5811076BEEEE39B5EB7E70FB7BC795E61C4A0D23E40EAC0ED7A818678E4D8FA43A968C85773A0888C2C9BA18A43247AE079BBC652151E6734EB604C030E36DED5C2F6D291148943C700B503BF46C80A57ED4E16DDBFA38796AD89874AF5A0B3581799BF42C10058782CE34548B33D572CB3192140696DB39B9327A19FC90AB910EC66945F9B347FD50DB1B146590F6A18BC9AD5528F7EC3FDA13B633BE858558C0CA7706D3B9375BE5B6B850A430BA2425E361B094F14240C6EA2A26EA12FE0E26773426622FDEE105C4D8FC15D5A8C6FC3886943A69B5C8278C1B846DF02E8BB4D886E991E545B37CD17ED57BF56D5A201F52C9DA02FF74F8CCC5D5E70D785A541D5EC2E1FD3AD0EFB688CD25A548B33FD338E742E8D013E5348221ACAC467BF4F5837831FAB77AD7CD17134917B20C04A4C7360CDA286A437EC3E636A984B5A02F35F76328320398881EDE58163ED6D74699CA691E140C7C6720D385C7D46AFDDEECCBEA520B909FF9D8F2CE5FA1D9BE8133BB7147F745F24D63426C4AF1ABBA217D1C762A1F05C029DE652780A527EA92A55CCE74E68FDEB3F73BBFD7B4DC6CF1B2717836E6C303807774A1AFD6791F573A1C751B1D9421E6655E6CB78DC90FBAE3A10BDD7E351C4D2BC33D1A83AE32B4DAA33AFE792CB67265D9F8FF629122998F211A8799A8A26F8023B343306856A7F7CBBC7BDA21EE58DDB32D1704BBA61C14C94671FDFD15BF77FE1C63755094052B9223D32DB26CAF724CC1AA2F2F894CDD1786F973FD53E2E1B1B9BCAEAE3D3744530AE0EE0D4880E99A8CF1E141304BA0F7760BB49F61FE4C9886AC9505DC1FD8CE0838EB91299C8A8F2D78A2F461176A4D38969C85EBF5D544C5FB090D0D5F47ED4B61A5B80E476A5DC56A4758DB35E6C80C42AFBA25316F1B641DD8C204BBEDB26CD9CE488116DE35ADC303A466898B1E6540376E0858B47BB988E904DA8CC50282996DEF73896389B0EF9E8F1992038E5535FCA94851988148D0DD910B22207DC79B92FF2DAC1CED1172ABC651968123B43149592EA559920E0727144F659AF6D3E22FB4462A468371A15C14174B06EF4C4D33071723226452E3B06D6C5B06D0A7755B04F23038B1BF6FFD382587FC0DE9F9C7C40B90EFCE99901F11FBDFC5F86C0D367685713F4E1BE150B4C86C29B0F435D86790D299598411B842FCDE8DE6B2F735532A8FC8EC7F967A958865FF37D35247ADABFB828646E3EF093798DEEB070FDEE15F407211388187ECC5710723676F3873C31DD01E4380FBFE556A4B82BF61047A4DCD80B1A881B8DF1F9F44ABAC9D12B961BE6A04E4E8664665FF11674BC5425E9AA6D6AA91A0312421F442C1C860CC55EA9BF80E0A6D999A710657860739B2293C4831DED2D08375C88142657526AC5BED69E6D953C0A0598F09FF44CF2838D54BBF280D4C3741465730F7D1E9B8F1619E84FF0CA208EE508D49F2EC242E2A7A6C21F2DA0A93C84782BF8B317CB00B1037CD40382B87567E3FDA8D78CE7D35356BD5F5767C20009CBF7BFF19E6A6059D0D6830D5A830F1255992714A53927BCC10804C2CA098F71519C9DD8782B9A21E986000E191CC8A44ABBA15189E36F7052C9E15753DB5CAF7B32F6AC4219DCEEC7C04FCC79DC3DE5EEB9B0582BAF43A9C73D5B568EE0FEE5C0FC8D73D3E695E615970AC63A843E4736FC8DD2E8B0AAF14C65773B508E3AFBC665F5F583E9E3387B6DF9629E85C850CE62E3CB832D6454B7B0590F61DC08EE0A8E2E453159F3C0A92650A2022484EDA840D4164E85E80B21EB400D8FD79B337E07FD8CD77BAC9EEEA3EC8C1828CD32AFECB6DBA90F8EED35DD0E5DBDFC3A9DEE989DDD017BBA3F0F645AAFF70EACFF05FDD5BA1FBB9FCB3DD3F393ED57AA7FAF3E77AA0F841392314EAC60575DD03D4B87E5FC3DFE2637EA9A40CE7A7281ADCAFB2273282DE68272AD0F7A7B34BC7B85ACF697D94B7E856232CA1988E2DDCD5BA058E20497DEF0468549A48B04FA4BD1A87B38CC7255A4BD0B3E9012638005909D4AA08BC89BFA2C5307E733E9CB2A0C4829BE147F480F3ED21CF4B53876A2491CA31298A6A281364C20D78EBF82824A35A0A7E59222B350F8EC26836F06FA6D866E5FEB3EA16D62F3CF5DB8EA079761EA73D94C43D15D787AAA37518C02AB8D57EA95E4D9CF743D1BC8F8908062C1AA13DFE5440A6018642518B88FF630C880564443B2A0059B18B27B085E3D080D5355BCFCED7B249B0DA17C0D7D2C4B5462389B91E294D02DAE6F19E6E410E208F00727FDFBB16E4E298FD43DA5E0D3DD0E55A087E17EF6E415E900AA462ABC4C6610A4735B79ED7046AF59F2704E9FC023B54129041852D24DFEAD6FE48311A51775CFF121F0C0FDD925C671AC5C96E49F15E05ABFF2296FAE78209EE414F61495D6818B0306119DCB8606076AA9E3E515D69408950950BB327F86E74E1F7C41DD5B8010C078CF851B749E122BDA9824D7C0A34E6E552C682D833ED0E20417D698237E40D3809D5F90BBC918D1E47B4F4142F7C344BE397CE30A47DDBEDC02D7475359DC9E8F13FEB6CB26889D1E766076758B981E43EC3285F72B14C96A58D9C232BB17549CFD6D2E88539A61DF8876BB193DED1438B43415BFC26FBB7FC8D7956892E04D8EFB85F237488F89C2E1A36B75A9071568E82FB28FB5CC98F47A3E51DECF37CD624014A2C6A95A038273CB860B408C8C2184F18F40E75132B1C35DB4C7CDC752F04ED804E3AFEDB54E55504B2441A76242887CC75E099AABDF3CEFECB0280FE1B24BBAB0C641FC42166CC52584E5F9AEA94258D376899EEAA9145EB22DCCBF9BBE73743CE38F3775BBB9260E3087E3A1D59501897FD5C0DA298E87C001B770C8D7C96E588D31B053814DE59AA5A08DE84D0FDF8857F949109F3415C5DBEFB2E3E54C68F8B75932600570980C8BAFD9DCCC56707AE76D862707FBEE854CE6694E5CAD867279C25BDC610AB7DA19B49C8342B261559AD3A1921669791950A7938B0541F6710666532BC7641B99BCA42EC0DBE9B74C33D9DA87CA018C4A5853892A7CE2FAA70D9B2F49A604B4C4630CBAEFA4ED7576E3A89EC7DEEA43EDC9D34A65D1F3DAED123ED30C86769FF55F7FB3068C58CE91EF0307CC1A9DE2038DC94CD53977B520EAA6CBE5D4877EC07407943BEECC43EE4D431C90CC443913E6E48487893A8EB2625C250D06BA9B5DFFB14BCD575A4B46C83443B9E252A65118DC66EFC1365C04DB6C5EA52A8919C71DB96AD1AEF0A5A5AF99F226C5AB541405E5403062A5939BE425C0E7D7A6F7B1482751CA2F4FB7104BDFD8AF2332772DFAB09181FF568D5F3AB8898EDA46D7ED7987AF5B4515ABA63C7DD9AA651DB7D70092BEEF15AF3AC771AC3AFFAC24040E5138EAFDDBFD3B0F70A2A8482501ADBC75ED859FB3945023EE6BF4E5B8CFB387218662E234100CAFD011954ABDEC4B6D42FF5C6D225B0F6AAF88A54209AB95E0A26C4BE0500CD7247F10F09BCF059DE425E6355F62CCACA710EE0E151F1BA2CE0B118D3B46B5B4F995561AD23FE5020446EC3E89AE1ADB7576AA6DECEF4889E1C8189F2139033B9023A1D82D615BC129E9B81AD6DA1960AD8191475BBEC9BEF3FA3EB9AF0FDE7E14854BFE77385E17FF54EEE2EE929338080FFD9"),
					paceSet,
					emptySet,
					emptySet);
			ePassAppl.addChild(epassDg2);
			
			//ePass DG3
			CardFile epassDg3 = new ElementaryFile(
					new FileIdentifier(0x0103),
					new ShortFileIdentifier(0x03),
					HexString.toByteArray("638247E47F618247DF0201027F60822266A10E81010882010987020101880200075F2E82225146495200303130000000000022510102001F0101013C01D8013C01D8080200000000223102010100000177027100FFA0FFA8007A4E4953545F434F4D20390A5049585F5749445448203331360A5049585F484549474854203437320A5049585F444550544820380A505049203530300A4C4F53535920310A434F4C4F52535041434520475241590A434F4D5052455353494F4E205753510A5753515F4249545241544520302E373134323836FFA800093C656D7074793EFFA4003A0907000932D3263C000AE0F31A84010A41EFF1BC010B8E27653F000BE179A4DD00092EFF55D3010AF933D1B6010BF2871F37000A2677DA0CFFA5018502002C03E238021B2503E238021B2503E238021B2503E238021B2503FA8D021E1103FB29021E2403ED04021C7103F1C6021D0303D6D30219C703DC37021A6D03D0FB03FAC603F16E021CF903DB57021A5203D27C03FC9403D20303FC0303D6510219B803DD68021A9203D7B40219E203EC01021C5203F327021D2E021B680220E303E646021BA2021A5C021FA2021B9F022126021B23022091021A61021FA7021AD602203403F0F6021CEA0219C1021EE8021A7E021FCA021AF902205E021AD102202E021C210221C1021B0502206C021C7502222603F118021CEE03EFB9021CC4021D47022322021AF102205403F766021DB0021AE7022048021B91022114021BA302212A021C56022201021BB6022140021F1302254A021FDC02263B021CAA022266021C5802220302228902297102229D0229890220A502272C0219FA021F2C024DF1025D8702237E022A9802EE78011C9E021A57021F9B021B0402206B022A5E0232D8023B260246FB000000000000000000000000000000000000000000000000FFA2001100FF01D8013C02424B0433CB000000FFA6006C000001030202050508080E090B15000000B501B3B602B2B1B703AFB0B8B90405ADAEBA06ABACBBBCBDBED7070969A8A9AAC0D60A0B0C0F999A9B9C9D9E9FA2A6BF080E96A3A4A5C1C3D2109597A0A1A7C2C5C6C7CB0D11161719235C5D919298C4C8C9CACED0D3D4D8D9FFA3000300F9FCFF0097C7F7FBFDFAFF003F9FFF00BD3E1F9FE9F8FE1FC7BFFF003FFBFE3F9FDFF7FE3F87EFFCBCBFCBF87E3F67D9F7FDFF0097DEFD3F1FC3ECFB7EEFCBF1FC5F1F77DBFD7FBBECFBFF001FEEC79FDFF97FCFFE5F67DBF7FE4BC7EEFF00A7F67DDF6FE5F9FEFF00F1FF00BFE1F87F5FF67DFF0097E7EFFE9EBFA7DDFBBECFEEFE1FBFC7D7D7FCFECFDDFDBF9FEFF87FF3E7EBEBFE7FA7DBFC57F4F9FAFAFAFAFCFF00F5FD3FFBF3F5F5F5F5F5F9FCFE7F3F5F5F5F5F5F5F5F5F5F5F5F5F5B3698838FB74BC362F05385BE135D3F9FBCBC6445FF00A7F0CDE915FF00DBFB68B2E6DFF8F8E51CB5FF004FD0BE181D3C7C05723978F16DD4EBE3735BE1AB969EF64BE6C4FF00DD7BF5C4C61DA9599698858A77A3EE7A4514B7529B104282F1A31A99CA020D304460CCBA7BBC4CEC68E78ECFCEF9DBB7FEDC1A4456FDB4CA96DE1E2194D105686DF046E1A662BA74DC3184F2D34D995BDD821AC195993035B34FC456FA853CA69FFBDD4BBA68B767271913C1013ACE930A474274369A622EEB1698858BA9B4BC2B901A3037BBDF194346C2990ABCBDDBA91A72EC6016B394EDBCA136EEB0F798444ECC88325E1E388B2AF76ECB831ECF88AEF2B339B05777914A14D544F42DB23666C4C69C25B4737468237788EAED591B69AB9DC6472340AC6B29E8DAB71E979A96E64809E043C3D354DA74D42AF3A3DD1114DAB163BAEC31A36853B8ABC334B53D8C427056FEA2D167D67B9D23C61B4D98CE14E461BCAC5DB3DB7421A8D4519CC42B7DC0F47131C1B58D1A10D352863528D0B08C422813E50A6C57CEE213713CA8EED36AC6732EDBAB7108BA8867227363574EDAED7144CE4DBA18B151959C0A2B6196B311913B416371CCCDE9A6517238A5B4EF471D3D1A112534F9850DA7A027A0C956C5724539B571353036F07744EC569ACBFD85B24B4CBFA0CDDDDBBCFD4CC841FD6DFF00C1A05714D9DBA7A334B6165C4334CC578784D884EEF4BC61B4F18A285B342B768C5A95109DA99BEB846534C0587687D7A6B63574567988DE8109A6DA15C37BC1B50D31A1B50645346151DA6D4BBBB6A108A5ED4C7BF178DCB30AF221615CDF736CF0EF42F7DD0D70CDD000DAC5D4BB5788AF1D82386AD64BB2F026C69BBC516AD4A91A62DBABD2D44DB23A8EF1CDA8DD0D811F203474168E3BB3705602627EC9668D6227ED3488FAE7B9D5E329B42B8775808BBE461D0C4CA6EC50C44D37B268965A2F8C201E560BC675EDAE515318785DB59776CC00337B36C7975DED62298C98ED6F6263472B2EE2B97C3C016E5D582CE38EB37B10A34C45D6BDAF1C74CAC40AB8D5C5638DC4EAC2D6F80B10A2651351959234D36D5BB453B98689A34F665943794CA0A130134DBD1B47736C43E6C6C1A6F4B1F306D0DD114E761A372AFD826C1A6A3F689BA11FB5FBA022C0AD846C7CCA136EE3578393D32988B9365654158D34D4D88BB5A0B0D8CA98CA3D3AD832A3DF533AF8F7FEBEFF003F8F80CBCE2CDB2D9E9E9E3F0F9673953184ECDB8F7FA7EBE9E5F0F2FE5BECA1E9DFBAC71F15F2CF97CBE3DEB87B99EFF3EAB3B67D3D31F1E3C4D71BC57C77F8F4DA6DC71E5C6BD49C6A43BFCBA9D971C75DF69BE55D0F8DAB144A6EF0D6C6312CA88710E1231E8EA4136558F060D2D47975BE0A6D310EA21A19450AE58E53591D39A34EAE7239DD1A0D16AFDB69A13D0F63A60A0AFE97A34FFA81BF741BACABC47423936EA5C58C881D1956EB599E995AF174DA365B74DB772C89B15DEDB71ACE312C4615CCF4C607BDEC1913AC43E37C67B6AB34D4B5897B49E38DA1C859A0DF6EDD35DF6CD05343DF5DEF79AF6D87C99B379D774FB2369AC04F3D0D7CBC3B79F59E3B6BDB6DEE5EFD2F3AF1B9AEC3EFDED6217DB131E3ADF1D2AE22F33AF8BBCE2787675325B93BF236FC7A602670C71748B11C97434063662665B00298E4D3604F036099CE08D0A1A1F30281D39EC6682233E974DBFF005BF737994474CF63D09D6E3DA8E760AD67AF59BF81AE8E55A685D7B6BE5C5CF3B1676C66DF27BB53C6784CE30BB6239666642FF8F533E459BE103408D9FA5EBC1D3226A1106FBFF2D4ED8E10EC0D836C75EBF0EB9D72DA99B84DAF7F1EDD71E6B8CC3BEF7F3978CF8AF0F7F5F4B9E05F0B5DECDEFC7FBFFBEFB7F2E9C3EDD3B74FD7AF6C1B78E3BFDEFE5DBE1E589E5E0BCBA89B8BA757FADDEEF1E7AEBE581A653E3B404FE535EC8814216FA8AFF961B685BB8D59BDD3CF54DA7102BA668E9D084F4398D34142753B9A8A34C41ED2A2653FA8BFF608F737CC430D0C5B4B3B84531BB6CDB94A650503393B1B1342747229D174D0C4F944F9B56534C4DA8D3D2C03B8710C429981409A7CE531D1468D58815BD1F260DBCA69A74ED88B1B35501A74726DB9E08634C6D3ACB0BF2F1C2687420769931AE5D388A0882831BFD363CDE4574FEA96EB3459A055AB8AE05DBF635868CBA73B9F26ED65A0FA1ACC58DF69F404D6EF367D4D71BC7FD4FF00E2F4705B0F0D5BE4E0DBD7A5C8560D2DBC2E3C7B0E722DD0D49C428D1DCA3B6FDB360D42263349B89953934DB4C2EFB860AE0896D09D3B43A729A2D0C7434E2100D0DA196804E9B563C346838AD451F598F0C014EE269B15BE3E58D911B1662B052F7DB17C11319469BBE9716A62D17744319F3F36E4EBB398E8F949B74F0DD98F2DCBDAF94D671C6F69E7189C79F1CB7E26DB534E2E961C9ED821AC437BDBEEBBBB666AF7C4F60692CBDF6E1FB5A8DA2CFA8BF73A177E55ACD5E7A67073D8F3D5613A8F2AF9E3B169A89DB4C29E6F7D2514D31D0ED02651028D02726DBA1F2873034134F4194E9A0431B5250E726CB46910F91C9DA18AE84F4B0D0668D03360421F36869DE4765350455A29CC6EC6CEE34CB7C5C42BE4DA627D658C73B88D4DBFCBFC7515C61CC6B6F977FE9FAF589A3931B59BFD35F7F96C879E6E37E7F0E3F9AD88EDF3215C7A7CBFEB6B5FA58BC6E718ED83DA75FD4EBFE1D38D8FA45FA7C35CBFAE03F73736A6387E1B7A7C66D3DB9E8B7ED91F7EDAEF3477179F1D7AE393BD4D1EB9D7ADC2980B3A3766F2988D0CF204D5DF2743E519A1DD681A7522B46821A7758929A605B6482B4E9B7CDE8D313A1DBA774F988040C88A73940532ED31F74886F1D73BE5D01A0DDC33E78BC2C5BF665377DFE5E1E9B805F2B50D7B78E3CBC3AE5DC7C9BD5F9746DE7D37F1CBBE58DFCB37FAE0C67DFDFD6789C8DBE3D7BF3AD787F86F8C3EECF97A7A5DDADFF5F974F07DCDF8FEBDF90BFD75F09EDDFE58C45B9D67D5BE15E6FF006BF739BA30677986D8726815EB8262176EAF2DDA2FA4B68B511D13C6632DB4C6B2B75B3B8B0210A32B7DF6569A1D3B408C9B697453A251514728898288718DD3D1D1C8B8ED0D068C0ABE1B582E727231EDE39EF776D14F3B72CE3E5E17AED2D33196E6176EFF001F30EDAEFB41DEEF3B5F1B7C2E9BD6DE7A995BBE99F44C4CEDBF1B74EC5638F7AE137577ACF4ECD4F8E7CD5D308B218A37E9D4A6F4B6AF93EDD86C4DC89F3778B57A0D67D864A6E36FE865007B9FAE544C93D8DB94185BF4D78BE6DA1375D8DAE60A2D4D24713E5B8B3AC6D31E8D333C567515B89868F4C2DD18BC53132858A2A717C85751C6851AB757C98DDB4342DC10984BC500EBCA4B8B39CBC172C598B8C5196B76A5A7B3B3552F7D7CB6B9BF6B166F1C63372BAE78F2C5F9ED97BF6EBE37BB42DF1C6D9D77D45BAF878F46E846BBF4473F0E180D0F7B9B6254EBBB426D374C8F90AD11346987F4B431BF6B4CB941F434C7188FF53D1FD637EE707461F8C8EF17EC10B5D9E24DA6ADD5E1AB53468B108BC0DC56A4B69BA666CA6D360B2F95F27426CE6DA28E63101A37C9FB5A6C189D494D31C57ED286511D3A6EA369A94E0CB450C4296F7888DB6C2856E9A31191374D03D51E39E9C6D14D0A7BF00A71D78D8D1BCBC5EA98DEDAF8C7CB6E3E5BC52CCF9E3C30539E7AFC3A3454F1F4F46E8BEFCF976434F79F0F18F9799899A6B7ED87DCF5C456AF10C7EC24CA76A7D414CF740C70A11DC4A6EB1DAD9BA9CB2DC6DB58158726589B797134394DB773418ADDB7A45B3D25029A14EC9A34F936EA5CB743E434D159B1DF268A15E8E86C01B9A08444C65086DA39B74C40D582BE7B669F0E0DA3945B94D63389977DC5B666F38ECF6C65E8452D0516638EF4F94BE36132D75F1BE15F29415729F0C29E71C9D0D5B8F41EED113579C7060E65974731FB4B7244D3FA043AB3F6B0F7417FFFA6007F0100020101040406080612161311000000B3B501020304B2B60506B1B70708090A0B690C0D0E0F1012B0B8111314176AAF151618191A1B1C1E1F212324252736AEB9BA1D202628292B2C2D31373A3E3F45474C4E5056ADBBBC2A30333435393B3C3D404143484A4B4D535EAB222F4244494F515254555758595BACBDBFFFA3000301B0CC06634F12C63091135234988463A24DA31710D9BA4C2E48CDF19CCFDE505094429DC098A08E33E536F333FC72E66F06337C477DF627DAE26D9DA24DBCA7FF00304C7EFF006E19F661130626718DFEDFB3CDDE7B5DA6F80F3368E3DBB56267199B4CB0C1B4F3F3610C976C66602344DA0D219AC164A5A2DB4CBC9B366B171B0D92B259C9729EF21120F7980E4D9224793C1F06CF7347585297148C2E02D0D9388662BD8E166610852526E90867ED121941C3963BBB89088999B4C066CC4705F387CF640765A324C477ACA4C884319C0C44ADB2130188C6D92265285CB0B171B9806CD11A5D079911A05B31B37296E42C4453578243469D423C4B2B10E851E0B0ED214789D5FC8D108474C408B311990D1856236FB7CF6859505CD34DB318D6C9B8E726F37FE398E48B8D9C0457CDF24C9B806630A67D64C04F2DF6CD662477CF9990F319870099D9FE1E508BEDDE38DE64DA79E3CB159DB7FACFAFC9C18FE1FC773DB9AC19C79E3CAB38F373E7FEC6FB4FB370A2999C63FF00BBFDA5796F8F26306658E7CC67B4FB7DB90298E60E2B13F86D921148B1289BC605988BAAD35B418E8D3668A0E43081D1B34E21E193D47583130458F02D87B58D38A562365601BBE79C94C68239F29BBBA4DED88CF2CE763608E20E29CED3EC168C8519CB366823004AC44B14A1B930E33B637CCCE48CCDB18C00B08E325F189984705F1324CE3CE64A09803671308669A28A182588441CD30CB0E05B346478638E710A08226AD148D31D1B948BCD63DC3FC8F56BC608C69E211A2998A2E10282EF051831584C684CC588158D06ECDF2E66FB1A930636ADB7C91B02C6673B2FD99DB62EDB1319B3C0847199E5BB30C7366CD6D036A666C14D998891B844A21C1D08C6E5929456176CD8B2469B11E4028707899BBA341EA6CC7B141F02CF79A1D66401086783EEB1D08EAC665D1A0BB0AC41BE4B34511213CA1A118473E582637CDDD713DBED0CF946EC5879F90EC6C8525C98D920B0D1184F2AC4485D61186626B85E0F362C28E24634D2530B1A346A1C5B21CCC9AB0A74214373B9A3D4788F8313C1EB0E466D821CC996240B1C9B062005955ACAC5C6DA0A02E26D88E36E06DE59CE487B6E408A6C63118A8D636226DEDCB96110ADB010F6EC1B670436C33636326D96B6DE923098A306779B502D1048458E14B819BB95541ACC08B66E5F30D4A391600A4A2E7108705D52E6AC7C5D5FA4F17D2F57444469E8906CF22045A69BA37C28E217CA669AC5649BE1B045B3B8E32EAC62D98534642968C56660A1499CE61E49BE3CA144262668998F911D44A3299201084016669186831589983A046CE83A250D1440D1C59A5B30E8514EA72231F8DF4B63DC3D03D6130EA7461071EDCA45B3324690260C5F2C16B0663926189659B8359B3166570B1A20E59BE61B234B5984DA244842ED8BA64216186633358B2E32C0773118D1E6D6433BB12F9CCDE1369BFF0009B1BE18D798267DBE7E7FC71ED8E9B24230F3DF6FDF026566F0DE903102B7F3625308C3CA66B3BF925131A64BB8E0E230D04A5A4AC1CCB8D3C4811F52BDE75876818F8330B16C932D62677ACB7C30C15941B31C463BE22D8A6882EFB5316366099F3EC526DBBABAABB6736C400B65CCDB7CE0B0C2359DF3324CB831099CD6FBCCD9CC4CE2234858BEFA1076DC85B0B4462ED331291C359319A2918534B81A6CD9BE3768A2E6A4609D8B06E8789DCF89D5FF000CC566CF050A12010D5C34C62A1C1B3085DA1A661C4740B0EF98D14C6366652244A28BE48F463AB0791468F05A2885869BE58E16260A1E64C402158A48E23338ADB24C668B33162CE73331B16CA851F5BB176B39585626305DB36667332DCDC61620C750841A2190E436208F26CB03A14D3D661428790D192621C84A666610D04BE234361A569A79E22DC02CC00A756C5668A6362628A7834C616738E25148AD13346A6098803C4739C6266629A08A6C0C0A4168C56688D2A58ACB71B0C2884C5D4585F194A58B8D050E0E75C2F069D52E977B43D0789D5FD5B310E2B0A343862159ACE665CDCB6509BEFF0063468D6264D8D1D112984C736963760D9284A613298B856499891A6E4C6336C5F0B58DB183684CCCE98AC93773612653108EFBE633105ACC1AC0A4558511BB734660E2E846C68CC360730D186ADD7A34F83A1D8BEB0EE68EB10C68E64623DB985253083AAD0B6C8B64A6B11B0A5CBB9E0A59B9CB0F028D485376094BB4CD8A204CED0DF3B2304994C449814B2871CD9B3829E45C7421669D0D11B272747BDED381A3E93D275BC502CD3C18D0428742CC5267CCCBA83636D8C42370B2C33171C8D5681A469A0A0ACC6140976E68DDEC201443546948C23334CC429ACCCB47916CB436733337C8D18218C6310D0630B2E0B1459B931B1AAC6175A4E2F2CEAA732884173AE22BA9C9CA511EC355EB6640E0D987634585314E8172C3469B598CC34D1745262E704B2F0485DAC707469631A479010D5346E422D9E0C704222F26E65DD8B1B945932C4D4D11B306C461468C6E53433376EC1A6C428E031EF3F0BD6BD9822B8F3C526813CC8D2805D214D11B1632CCE29B91E4366106166E42E7229E6D63558372CD8C3A9D8D14998428E5821A1A2463163A36C312CF0294237266CD117CDB26B8B2F97D792E6A8CC3BFD79B3A371CBBC6C7053ECF2FADC959D5171E7FED8FAF7C3939199FC3CDC879F3C7D9FEDFC7FF00DF679ECF693ED4EE32CDCEB078A5DB6CCDB737E2E76F3DE33666EE9F63E73606C81A79DB31A5211B231354A1B1D05E2D0D8A63D18B052822D30E05C8408C6162366181230355B650742CC06633C5BB621A8868C777CE6CD9D1818DA61DC998E98B64867131377471B636DCC9E7BA06AEFF006E598F3DFECCBBEA9E5B4F2ADB3FC08EA3E7E5F5E727B4FE1E78E3BFD9B466769BEDCCF2FDF171F67761B3D60DB00C61C9C530B3EEB721C9E087078B72C74747B0E6FA4BBC0D0D5E23459A2ED08373A21D1D0CBA3A1629C0438375B11EC639C40E2511BB0756C9467BCC7817DF74ED319DBC107C0A3AB7836020F7367934E63C4B662F37417F11721D85DE8C6CF2393628D5988C39B03A37231DE01D084C79E1A3A332B82CF13315DD8C351980A704381850020F1730CB6730E0609B5333CDCA6731CEDD028DD13B184C3E075722654829821A0982B24D8C160996B1310B1648146845BB4B4DDBA345170D078A684783A9C4EE43A1A3459B3AB663CDA5421C823441E6034E70B1A6F86C902B17170B77CB2DC718CEF07358F6B1BEE9169F3DF73CAEED81C826DF5EFE66BE6BB0EDB7979677D5F3FB73199A77E23BD3817BDEAFCD60A4A78371AC8E788D010B2B7CC0859B9A1B165BB4DC0B0EA68161D09868A2E761EF0C3A364A78B4D04DE03735068858B172195DB36610D08628CB933C4C586BDAEC5CB630C2E6AC639211B3A1AB6512EB48DF19D9352875C3C8B23E00C3ACA9729842176090069E059E0468BA7626A74791F09CDE0C7BDF71A393ABDC4454A6E9A99C62C30E0980A38B0CAD666685B190F2B38C52E86578068C28A21D3346870660BAF31F48D30EAFC422628E0D159CADDD48E098798C2EE87360F630FCE773E93563DEAC7528B9B308D9B91214CC3930D9B9BE482712899B198C354CE3242EBA193361CC1E2466789C0E67112E7691EB388C6343C98C42E704A29B9C1D5E8479BC5E6EA1D1F807DC399ABEB2668EF7B5BB3186E73662CF60CCA8C68E2B9FB03B0A318843B48958EF0A1E6CCB9C9DCA3961D821E93AB81338C4CC062EA6F07C1D4D5B94AA6868C7910D1F494737521C1E2D8F48779C5EFC45F81E2E4E2DCA26E4CF696DB2D8868C21EDCED4C0392B03B9C1588F63318CF7004CBD8C1ACF7E0EB0EC4A2E71C384F03AEF1EA1F4E5EF203DA59C43B843D186EF6903B8ACEFE0D3E2BD6192068706EF7118FF0089D649F14F121E0E54EE5618EF30FA17C0B2B4734DB0C7B58C7D4F58510CF12F88C2E5F16603639943DE421DC76163FE45DF4B43E03DE531B3C8334907903BB88F160CC343C9C476174751743B028BBC868A3B32D076914EAFC90714C391048443559908F6B1B64F53DC423F23F8CFBE31EC62D0C3891B661779B111B376C3884C773B345DD4377422BA24DE25231E0630C3439041189CF3099C59E823D61DB29D82F830F51D741F13C47D2BCC830EF57D0D8EC06247B5F4BD6058511E859EF29EF7F33D5F8FBE78BE2C3D053DE5DEF357930BB4721F50DCEB569FA9E0F07FECF3753E83C487E829F59C47E1757C4B1A3CDA4FE863A1642366EFDE7BCD1B8D14F13E31B3DEEAB63402E51F5300B30BAB08FC8BC0A7BD8F4346C7C6DDB3DEFA4F95EE6EDCFC0723BC3F211868F8BFCEF029FA4F7DA75688D9D5F74FB8510E4461E0F8BEB4EF3F40F81F1BF71FF23FF07EC6E7CEFC0773F39F11A90FBA717E179BFF007757A0FE63E03ABC87CC7DE2CFCCFE13F71D65DE67D476176EBF53DEC2C7C07D4FEC3EA3D6F3357EA62D9E4FE8393FDEDD89D8FE23B08C07B8E49F2AC38AC221C0FE53B180A47DF7E922406113A3C1F13E141288F168B05C3DD3D659A617C746EFD27260D3A1A3C1EE3E81B8F22366059E2C69F908C2348F42C4584353D63CC8C1A2E732C58B3CDF507261108F6814365E6DCA7D4F36118EAD8998308C02E717D653C0A286EF01868BABF436489E246E689A1F216128743BD1A625D1B3F1B48D3DE591A68D5F5BABC520F2746C51F912C91EF089C4E8FC6FAD8713E575687D6F045B1A9E2D0EADDF02E6AF07D468D1658363E2061F19761C5F536341D5F78B1DE737A0FCA30698D2707891D18FCC58A347A2736C769EB6E689F23C4F88691B10F13C1FB8D8A21A173D2EA7CA59E07CE7C2446C3A3FEE1B1F1B7799F034FBED237444F9DF7DD04781F01C4F89E0687C47D22231D5BBF01F23FB4E2FA5EB3A7FE5FBE75B83FA8FD87B0236BFFFA3000301F7DD578BFD8EABC5FA8E653FDA68C62C29D5746C703F3BABC1FE5789C5A3F02BEF91B1AB76EBEF3E83A1D01FF7BC9F17EE967DE683B8B27C458D02C1EF977E87830A080685C234F47DC5D0FBAEA1A045A3C163EA353B1852BA39CD1EEBC1BBC02E46CE82AAC22D987B868D363563768EC2985829F0752C598BA3A14479BC0A02C7CA68D30B14408F07907CEB02ED3A10D181D0A3C1F41414685D6347323F33EA0E05969A178BE818C2EA87062D34428237661611B36343890E24605CEF230581300B0B2B1D5EC33D18F16E53015D7018B051A3E828D587029B94C22B72C11A2829F12E47D0475236008C0B14052B639045E2F46E1A02D0046046602014479BC4E8505DA230D08011847529A2E7BC59D08C08C2160008C08463657C0B3039B0E4478010A660A58C0D5A781DE59B0460768598408CCB45CB3E96CB1A69B070563AB0D163E2A1603D00118456963185821AB13D0D9E8DCD5A1A08C63AB60846E1D0BBC18C2ED9631B24022C6CB1B162CAF4389CD688C63626742975563A259E8B739BAB4AD88B1B04630D558C389A3DE68531B3081A2DD6800BA91D5E2712E1A10B1A00059988AAF7ABD32E8B4D34E8AB0A4814117931B8160EC74234B0D0A23CD56C58029A7D4D9B34595A6E16288C56C1156C40A3EE810A6EAC2CB4B0B60D1F43CCD48F4205869852BC802CD9BBEE30BBA341C1B3AAC75682CC69E4D030E0458DD611BADC8F06E85CE07A08EABC1EC0A6C5D853EA2377E10211D18D9A481F71F7CE405F210B9E05CE6F8B661A3A14C7C43B175383C82E4028B108EA7A9F758736CD9A7E36EFA96C53600811E87A08D1DA597C5B30EE392C23DEF20EC3D0FCEF13BCE2684343C18747FF006BA1A9A1EB0EF5E4EA6877BCCF06EF681A1EE3EF1EF3F71743DF23F4965F80E6769C03BD6E1F29EF1D8E8789C8F42BDEFF00407CC10F8CF17E27F2BABABF1973E07AEF9DEFEA7F6BEB0FC0FEE781C9F6009A3F9CFF0047FCCE0FF5BFA4E6F731E0FF0089CDFF0081C8F600BDBFAD7FCCF6179F3EC01332CFB00958F603E43EC01EF28FD6FB0E05028742C68D9B30FBEF00ACD143A34FE30B3038377B9FC6BF23F4B0B81AA59F13EF1661C4A7DC2EF023EB16252EA1AB1EF0F9D781459FD0C4E02733F20C2E3AB77E93BD86A714FEF389F9CD1BBD8FF5360A2CFE73DD3FA8F4BFF57EA3E13FB9FEC23FC8FCA58FD2F13F6BCDED7FB5ED7F31D764A3ABD1D5FDED7E63D802B07EC3AF83EC01936CFEB3AC83FB9D0FF33F50F43FBD6E47F511A3E27F11F31F804029F88EF7E321FB004BBFD6D34E6C429FCCF1160846CFF31C1142C51FA08418162112E7F494D1DE7E07D0D2D3622468FC85DD0ED21CDFC8C33AAD0C38BF95B302308C683F9D28E6B76C7E138A9A8C58D30187E16E514966068D9D1E87D27463A3ABC9F95BB76EC2CD8B167891FA1D0B0C29B053CCB9F4A77908E87268FC0EA91B068704A754B1F19A305234598F268E0C3EF091A5862CAB77468B9F48D9CC28A563642EF121F8908C38AC35744FC04614E8D3F95D444791EA5FC49A18081746EC28FA0D44A1982010A0B1F994B0C080420430461F84D1234DC298F02EFE2288516230085CFE96918724B842CFE31D0A15730B916E7F33C1D0BA47F290D17FC12E91D163F9DFF47B8FCEC0E21FF122D3EC07F37ABB1F41D5D4FD27F69FB5E075867DD3B1FEE3F9DFF99D788FF57F5BD638FDCFB00451FF00D3F23EC03687D8031AF5AF3F41EC01387BC3EA399D0EB0EFFAB76EFE969FD6735E8FBCF361DE763F901F43A1FD051C0E6FE534743D4FF92B0FD4D2DD3FB8D1FE63E87E6389FA53D4EAEA3F23F79FBEFD0EAFF21F79FA4F49C1ED7E26C43AC89F33EC01222C7F80FF00D1A1FD231B94FCC7D0FDF3E57B8F600E53D7D5F628D77FFFA17F6082256CA10E81010882010687020101880200075F2E82255746495200303130000000000025570102001F0101013C01D8013C01D8080200000000253706010100000177027100FFA0FFA8007A4E4953545F434F4D20390A5049585F5749445448203331360A5049585F484549474854203437320A5049585F444550544820380A505049203530300A4C4F53535920310A434F4C4F52535041434520475241590A434F4D5052455353494F4E205753510A5753515F4249545241544520302E373134323836FFA800093C656D7074793EFFA4003A0907000932D3263C000AE0F31A84010A41EFF1BC010B8E27653F000BE179A4DD00092EFF55D3010AF933D1B6010BF2871F37000A2677DA0CFFA5018502002C021AB302200A021AB302200A021AB302200A021AB302200A021CEF0222B8021E07022408021B1C022088021B800221000219D4021EFF03F326021D2D03F37B021D38021DCA0223BF021A43021F83021AE8022049021B4F0220C503FA22021E0403FF4B021EA3021A9B021FEE021D7B022361021CC0022281021F41022581021B8E022111021CC4022285021D2F022305021F940225E5021DD80223D0021D8302236A021CA7022261021C180221B7021F1B022554021CE50222AC021CFC0222C702204B0226C1021DE00223DA0220310226A1021EEF02251E021DA30223900220E4022778022084022705021FD8022637021F4F0225930221AD02286A021F4B02258E0222260228FA0221980228500222E40229DF0222980229830221640228110220F202278902232D022A360225E6022D7B0223F0022B20021C4A0221F2024CFB025C6102229A0229860123CD012AF6021E970224B6021E63022477023494023F19023E5D024AD6000000000000000000000000000000000000000000000000FFA2001100FF01D8013C023BED042ED2000000FFA6006D00000201020304050605070F1413010000B3B501B2B602B1B703AFB0B80405AEB9BA0607ACADBBBC08AAABBDBE090A0C9B9D9FDB0D0E0F9EA1A2A4A5A7A8BFC9D6DADC0B10111E303334999CA3A6A9C0C1C2CBCCCED3D71F222F363941989AA0C3C7C8CACDCFD0D1D8DDDEFFA3000300FC3F7FFAFF009FF97EDFF1FDDFC3F0FC3FD2F8D78E35FE5F87E1F4FEDFD5F9F5FAB8CFFF003FD3D7F4FDFF0097F3FF006FEAFD5FF5FF001F8FE7FBFEEFCDF7FE8FFA4FFCFF0067E6FCBF6FF5FDDF97EFDFFCFF00AFEFFB7EDFBBFA7FE7F7FCBFF5FD9F9BEDFEAFB7FE5F6FE5F5FDDF1FCDFD5FD3F77DDF6FE5EDFF00F57E8FBFEFFBBEEFBFEFDFFF00DFE1F1FD3FA3F37E7FCBF9DFDFFB8F5FD1FA7F47E8FD3DFF000FC3FF007EBFAFF5FEAFD79FDDFC3F0FC3FD1FD3FD73FF00BFBFF87F0FC3F7FEFF00F5FF005FE1FBFF000C17ABAB3011B668F03AED57AED384533AE3FC3E3ABC591FFC7CB0E4A5FDBE89D28FF1F46A3B67F689D5FDBF4D3D0AFF00BF7F96C23FF638B94CD7AFED9612E71085DDD4BF23C99C6FFABE3358ACF2273EAC5B0D9ABBA30DE1A267241C329828C4C38250115B5C385CDC088E39A68892EA5935C18D76CFCA6EF43DA6BE95D73C719DB5C4D5E6C8DE02EF920C46E04CBA594C316DD0F4BD2E2C1B8756C6648D40898B65B1B71BE5BB23A9E25152F383DCEF4F16F4A5A7F8CA20FE2B3152D87C1529E5D44B8F4D4B84D1BB554CCDE0C11604A6AA1D3298A666C584A66A2470B93342BC977CE44993C37B52D690C0138DCB05A35810C1CF3291D54D60A8856F94AB4E35CDD884ABD5E2F1C97C8384C5742A76D685E8545767779844D9B95D737CCA48C5C32F07675BA1288AE0178D4CA92EDC1836E2C102245E97AEC9DE0B18465ECF172F74CED7532471BE0ABB8C25ADB65B58D6F350C31832B9305C18118E39E1F3BD83A53F598D4B83F5B1314BF88C3FD1A0A2EDB7EAF44D24B80F9396B32DD17823D6AC94466767AEA32B62C554E94CB618505A8C1EAB12C4A8ECC188CBC28B464C104F36E9D831586594E065ECCCC367146BC884B826CAB1206B174C2078D5C484E76BA46315C0306AC5B897836D58EC2752540E8BB3860083822458744D92A6A23820C08E065F5AC8A69C53810DB2E1BBC36614E8CAA8DB2E19A7AB06055E98D3E5A9DE5558E14F3E52E72D6B8B3E05E6677C97F88D69B87F294259F65C765655F50D469BBC053AEB7AB5BE7922BAE6FA73DF5A33DFC180A2B6D73660ABF1E39D5714DCBE65D8A6F7CF3CDF13BDCBE7C74272D719B873DF1DF59AE817C95772E73AC2E13393995AAC9030E2EEB9D5365F3AA1CCA8A56569BAC8F210835CEAB3632EC37BA66A7A3E39A52714CA76BCF0EB4E46F4A562CD633458873733C635A462EA99C6F52B260AD9D5D4BCF6CCE2B6638217733BE7AB8A0DAF59E3B563797AA654219CEFE9BECF870E2D1ADF7DF876A73ABB8B1EF66E79317AF3761E412A542BBCB3CDAB1689AD1F5DD37B5FE3521FEC53F2156B06E9F34974CB2AABAB2E530716CA8406A5F6B51898BDB277EDED468C060976CB33A378B12129CA1AE2FC1C538BB68D0F1CD77CDF7B0BCD219E68B6B9CE73A95E0CE6E174E5DCE6F8D5E5B435583BC376F2F39E2AB55A26AF87545F0CD3DC88D2868E9C4AE95021184B8B09686121025C231761860CC5C10232A3143858C3614BE8CB86C308461B6653123691842AD0222080E18663183013CB7BC371682BAB6663170C7C848DED67F12103F1A87F3210763EBAD56C413EA46C6D82F9DDA540C150705C2F32CA6B0907043DE536B4D42B66339F77DC5294A7152B5E9CFB7AE82E9AC6472BBFB71E1F5F71C68AC24CFB71F2F67D0DA8C66F3C5B99F3F6F4E7D39AA617DF3ADADDFB7C7BEB38CB0E39CE499B99BF9805D4BBCEA6658DCAE230D4E37F6E22E023B098E7315AC318A24D512E88108E0A9790A8E1708B10C009085C461861B163823D0950C370229B18CC3616B9E4B1E951EB5A9AC52A8795D38184033F0130B14FA9D030B8FD8913F91FF7B66C47CDC22B2F0F9AC2E2C58F4A9ED2BDEEA899332885FBD6B35F49EBEBEBDB3B918FB73E98AE3E77C64E3E57CA6BDB9DF0CE2FC4F7D7C6FC6F39F9D71C8CEFBF6DF7F9BE9F395DFBFA7245CBCEABD7B9C7ECBEFEBAA452AABC5FAFB677959D67621E2FD75AF5EFDF8AFEFF004D0CA95D9F7ED7ADE6BBF1BF8EDDF2A1E99F7F5E65D4F4FA6FCCCF7F95E3D223BF7E77F7F5F6E2F39783BFCBC12FD67A7CF8F0739A993DBD7BE6BE6D6FF38EBBDEDDFBEF774FB83DCD9E9C5B5DBD8ABA7062C0B9DB7F7C77A30E1952B7EC7884D61B6062B7ABE71455698D0696FBDE04602CA9DFDBBF3D1BD59D5F7EFAD756596AEDE1F362818ABFAD75B1FC6FF2A6CC7EC4C25268F811112D8F95335DFB2583B2ED66FCD4CE1975A08ED4FCB45986F3C9AAD178E6B886AA3799DEB44E53C57759BE8D15CCF1CD77E35BCA097C5EECDFC3C3DDEFAE0C6B7CB66AF3AA0F1E9F4F7E43594216E6B359CDFAFB144E6B89ADDF917CEFDFB7B76F9F25D55B4CF783DAFE3F4ED7E9EC06FE3E5F4F7DEBBF3C672DCF8FB157BEB3DFE5F1F1BBEC6573EBA1D679BF5E355A34EF7557D075F2F9F35E0BA1F209AE1EE7827ADD6D71705EFAE3BE0B8D5796758CF7D83483D58D6B673A99EF9B17A53B258C20AB07AB4914235F512F5B1417F58B0827D8403F9EA9651DBB715F5657290D55AF55B872CBE560F44949431D91D88C21E6C6BA3D0551C53B0874558C585DED5868C03708F5086CB80C07423B538B7A3D422C5860C3B0F462986F060C387A94C7039C291BDB30E97CC6334DB813666E984879D60E4FAD96186AF060AD97669BAC5301C66213BDC6A187A34C779574C30F44C718D4EF9B21F0B990E8D79B9F17C87175755A8F955F3ACADE7BE793E19ECF7BCEFEFDA1FC4F6EDDB43F8C8FFB48964AF838AB6E7AB08F51B792F5EF8D4D5F5E7BD5D73DCE0CD1B91DABB1D095C64E37DE9C71C5A38265C6B5AC1EB4C3A1084BDC99CD41C3B56134C330761E8422C560A9831994629C114205E2E265EAEC94E07A5F43212A21B5608C001C06D5B36F3E9B95D125E42AEF5766A2C6D529E3E3F2A8FB77831B66B59CF3AF1A276EDBB705C1C9986DA97733C5E1DF7EFDC952EAAB7F5F5A5C37DBB4D2544E37A5DB97D756EC679FEEF1E3BDEC67BB0837DF89F3F9FB3D2FB5C298E67BF7D6BCC6E6A167AF7CFD4C2B52AABEC10961FC83FCA17AD17DBD773E0F3EF55DFB7D3DF53C79BC76A95E3B5EFE2EEB3D139E77D5AC73A2FBF4AD170D46EC94DF061A231B76BC2F470C665AC090DAC306CB34838B5A2FA1565ECE2D87D4908793189845C38486C757AB8361E97D2B35822E70608F53AE9A8540D9F2C91C376B8A4E948C4C1B6A0E2AFDE986D6C33D35BFECBEE558C362B675F3F68A7C3425FAFAFD386B04360BAB33BE96247CAAF47ECF6F93E38B4F22E9EFFDFF00DC19D03E63E9AD7B78AE775A7CCF6CFA7A5F6F1437F519F1DCD5DD7E4ADEE3FCE38487D74E9A38ABD579139AFD9F33390BF5F03D2F89C6BD770E3E3BF1E3B476DFBDDE1D31EFEEF67610E844AA235B3176D4AC6AF3D143AAA25B5D01E8476287021876CE157630E0C2743604C3D32209B31D98EC30C3060740B8421D1E8A2CB70E17A99D5E0553623535AF4CBD070C49A9EA6A5387A03738D6A87BA21B32B35C7A6A67C3AB3623CF7D73DF8197C6AF64C67D7B7C553DCF8F6A6DE97F4F7DF7F611BF6AD9E9C4F6F449DBE97FAE21D5AE1D66F3E3D3E66B4F9D42F8BEDCFAE2BEBAEFC77CE7DDFC4C4BFF0081F52B0269952FCA863DE7016C6FA2DF6D1BF6CD56F90E9AD5E2EB3995CDADB83BC088705EFDFDDE217DCD8C196AB9F5DE1CDC5D985C21C4398F5B46B5829F812999B6AC8E565E133ACD73995733D133755A2AA2D686B15C16448B0BABAD719E38AD182E5184E7D735C98225EE5E79D7B877F7E195697BFA7CFB710B0AF5F5F6E2ED66FC7ECAE33B678E79CF1ED6F1EFBFA36932EAFD6B8FA5C37FEFEF07170E38AF6F7DBD7E4CB8CA2F9EFEFEDEDB73CD5328BB95AEFDF93056A3610B52C3C8970851DB8AEA3C31E867F110A0FC8FF325FF0022443621E4868ABCB989AEA62EE8B708E61B24BC2A55F0C69C65CE18D5C3552AC84BC95875AAF59790C158B22C739952AD84AAE951A942594DD9D4E2AE58D1669D47A64716C251355DFA0E1359C29669B2A39E5E317737D675C76B2505E2EE5CE3BFA77F4F8E7447B78CEA651DFBF6F6CF8D670CE7DACCD0F16C408D62CCFD2AE576EF9C1B5477F1FDB75733EDC6ABCDCBE325E3BD09748C1A7A9662A256D60742F0E0DA87E0732918C252756ABA383EA420E18FE3B88BF881FE85230FB168C156F95161CDD50EA53D2ECD6865CAEDC5E8C0341861533BD7420A185AE4CF6A8CB53AB2C74C2ACE8E0832A103061A89B1729BC3E4B2A056C60DAE0EC4A2FA21B1E61D755169E8C635122EC3D062D8E0238482EB59886D4C69780E1AC656A18D667B5F29AD73647A577E3D2B2959AEC8B86577F7F0CB7BFBCE2E2C7537CDE179F6E618619D72128BD198218D3C99A7145AD9D383587677E01EAC76A6ACB87C2AEFA53F512F2D527FC5971FC97120C23E6E625319508F9A2CD032CEAD4B5356D1551E8B844D25170844A7CEE682F4C06A23D1A6D6F0317A10B8742FA318DE08E0300E0536210C2E1D9B6242FA8EC2757A8E42D74603A0109668BB63E661232B1716386825C14997328813598D470DB87A5465C6319417B5590B8839D73E4D38211B1A4E8A38AF2597D0737765430551E55C04306B279B848C619FB1111FB186CBF908FF00BBFFA6007C01000201010404050907131E0B07050000B3B501020304B2B6050607B108090A0BB70C0D0E0F101169B0B8121315161A6AB914181B1C1D1F212224262728292C2E303437AE17191E2023252A2B2D2F313236383A3B3D3E42454647484A4B4D4E4FAFBA333940445051525354595B353F41585E60644C61ACADBBFFA3000301D48F611A4B1C9E07261300417468BAB1C217C074C2538B9434FF007F1DDBB0A69C57F7C61850372399D33E42D910730CCDF1EC7C6C19630B6F8E9D3A65C46300A3399ECC1D209463330560FB30F46B303A4C50C3C9F204C45DC988C68830AC1100852E8BC4E05096288D1AA45B1A16585247313535614D88316F85686F886A01628B3AB1CC6EF17811EA2EF32CF9DFADED23761DCF7162068B972D9626A1880E7560643CA311CE0A6B31C74058C324CD9C466F8998E058E51630516094408B08C1A69B6288D066C1609E21088CC5C69CC742622D26F6CD1121643885D48EA39237C68D24205876356CF01E0EC0DDCD9B167806811D4B1E6214F25185DE6FF071DC50C6C71C511B31D448D3069D9684844236629860250C4B63CBC937ACF8EE38C466426265E93A78CC8146664718099DD2BA42331E4D9C63099A77E81FDA63CA18DF065C67769AC1FD9AC51998983299C74DE63A418E77DF1D03043A74DE886779E5D133EC9D31E5EC84C3871E4EF4F4338674B333E4FD81BCCD017667CA6336CB45140930D9E0DD8B6C423748446C38A231B837706EEC10D710A2F87860234461C06CD987121F68FF0003CC1DE1ABC4C44DF423770F4E98A2166961330102ED920E37CCC65C308CDF00E265C34B8A31BE08E7A6776749D0B398D0D794339DFA0D2574471E537778A2193383CB1BF90632CC9188433840B3E39C81585A71199161D01882444A048D019DE28D8B64861A5A68B990B6614AD9B1969C422D9B19BA331B3B33265D9D0E4E781C33C5A353896610E4E87D207700AF522DCA3830357905B031E4EE5C030CC14431D3A0CC68EB99977DD85195B0E63E39F6621186B9C633F63BE234B7CC6D9F1CE375A5A3026239FB0A1C0B4D00631A2C0D184C462B9CB731A0EA71634C781618C63763A31A28AC588721D46090D0858A68D54D46116E4745D18C5E66AC0D52CC3BD4860EF3FE9F68B2F328A229C0B10A230D48160D5B91ACDB1A0D9A4485172CE238CE357641E94469870336CB76264BEF910847899DD69484356745B09B259D04EB69D9A399B31D4EA3DE38346C7147BB10D9E23A91E479DD4FFA0ED5FB4752931941ECF173668D9C4E8E5CD34C0A73098F2CC2B359685DC70EF80B0AC207406618159CDC5CC1E86FBA6E64B20862C611702CE8E71E59099DC5C6E1598E7A60C5159C6A0A17CA971098211A6361288F05639BBB2E8D0E789B172E1AB635605C8F30DB06C1B05DB959E19D95E4EAF53E0E6D0E8F53A35978F4A681A356B14316CE8AF9461128B93266633183B04DF20E28B6753D81E5E3999229AE33EC20530C9668E9E300C9968B34278F93AB0D772B3748973428B1A9A118D9BA706E6C2711D9E0D3B1DCF9DA782714D91D8E4779D6EA759FC5834F31286EEC534E18D38D0A04CE18CC45D48B83278891D31868E9BC3A1842658AD19C4CEEE3473AB89D375314E28A63199C65338C74265D18E2749998BADB1E5852B264CE19D231C74C6718AC4C119EC1264AC460904DF7E965A66EB5898DCB64155A5899C35BB8DDFED5898231CD8C56738086321C71D18564E60C2E39A7400D8746618E8C38A7534F5676C9C9FE6799ED6172CF1715BEAC2F998C749820D0ECCF28C6B392942D91A4718A660613064CEEF9164D08074FB27961B9A6278F49F62E028BB8E98F1FEF30CC7431B8D97CBEC3C9DCCE066704CE71969988266632DB174C90C94133338ACE6193A0B061467158A22C5313317158C918D844483BD3C92D9A1D989584CC29BA1C023A3B2700E0BA8721B878356754E2E63728D51A7417658AE216637C97537C6F33869CBC199DD851946E947484C43A10858841A79E69B90A485B0C2E4488DDBB0A284B867428B97C166F929DE0384897231F2B100A5B09319682D8DF0D62332D10C332964809A9BA59BB0850C4B2C6ED98C2CD808E8EAEC40C18361D5A38E69ED2E560F06B4C6A7115A4380C54EA7132259C67416816C688D8860A3386CCCB76B0F8B9891D7210810981A2C2108D661C162119801A216C58997A5113389D3259C6FB99E99C21808D9CB37F1A21E5D28A73D3A4CEF1C3EC6948C2B3099238C6234534519C629A23C311D33668B261D123A2F60E8EA469B116173DD606CFA4FF00C9451678B4D11A5D5598894736065A2CF169A200D984C661319631B9478C2181AC0944C6F18CDF1311DF7841E98CD10704CE6E987302B33CA19C853D231CAC238C66EE37DF77A4C131D3CA61CD159F2378146E9D331A3118C01CE2629636686DD1E0118C0A58D8D8D5D08B0D0F394ECC743B08D34EA3B1E87E23FE8F41A3D68BD86834F1686362968605171E467823A3C514A2D8CD2096752742219231D0898A2375E4EAE3A1AA6764C5376E42332F04D49BF70C68A3B5B973ACA3A8BAC7CEF13EE3E0EAC7B91C43A9B3074752601605068C1186A0591AC90B068D63456629698C6C18B07108CCDD782D3306998D98D31B11BE72365A28282390A72462DD71BD608291BACE9EC9E3099860C6CB055839E8ADF397637C438B4DB7DD346B116EC5B253A39C98E2366B30A6ED0C7636C6AF52C6C14F98E4F83B94763E645A2273344B3CD766EBC08B12E448462376EEB962AF36B0A0E56EDB0D80816754A300D9E0684341A585F31B310992C563376648D035847816218381B3087028BB48DDA3B9E6ECF07B4D4F7DD4F06E2659E5BEF8E493CB1D3A1E3E2E357C7FB7485FD9BF97F6C68911A498CC77BB0D08D13C75626A2DCC0B61843668BA516298534D318585E4868712E0D8D939251A9C4EC68752C5CD8B974D819BE2CECDCAE82428D5338CF927268E87B2B07016BA1D1C34C755C7B3A7B23884789B9FFEEFE5BC206CF963A3FF00E79158783BCFFF00B3BF4113967C4F2F1F66F9C7578BE5969EBC11FA9A3243926F9F2F1E98264DB29BF4E9820627B357A1D1B64F1FB09BBA17152B1FDAEB41A1418B91D3108D3C85CAEC1CB3668E2B62059E4C2EBCD851C9BAF074628370D5B1C8A0C0C69D1A71330C58A2E1898DE960707A39C210E21E3F643397CB3801B61889D3C7D9D3A5DBE373A6EE1FEE6F098D3331130EF324757278C618F2F1DF18E0D06338CD3C4826714F5E6BC5EF4FA8BB80E4B6C994875106026AECC6133C5B10B961D1F53A36389A1E639BC1F718F023EE11BB6393C0F43A047B07362C6836319B3CCFEF9C872609858DDD0B19C39998F14B33258E0E761E388C5876632E32C7AF181FAC683993308C66347315234D74D8810988181B8B16EF11D561673721A1A8421C1D5BB6346EF7BE83B8EC3BC3010EB73DF88ADDD461621C508B8998725810FED85E258499851AB5928D1D41330598A78013258A76CED87989D1ACC7965211EE5FCCDCEBDF0C55E01018C626ACC8E4995A42C4224613772D3018508D86C166C9728E072763468E459C147068A3ADF497292C9C5D531076693318B738B443891BAC29B0B818C01846C62188B5E333A3985C73BD30AC1B97267A58B399BAB0DECEC2DF0AABA005C321B0C2EF6958FD8D9EC46308D1B34A59E4B4C63459BB18DC29BB06883379BDB2D602E6825C28B2E634C2878BB014DDA28BE12371B145CD56E5C86818B3A31D1D1E4D33C6CEA884C8E486CD19C0CC090DEC4CEE8C6C2B0D13A63808D10A2C2629B846CC47234514563172D8BE0296C666373B55E265D5E691811EC211FDCDCE658B2BC1E19A2E66C6AA06CE88A9DAF730B9DA76963BDEE3438B73534756C42C79D18EC53157569120628076176C94DD01A1A350CD9988C630B8D9695D183701383A98CA1C8B303991A4E6E8F5963E9767924210A3825842CF06E28712CD97964D08172C8F70FCC6A312C53C4E46CB1D9B960ECC147274489D99CA8F26B38CF41CA906E23B8DB7267548BA18C897239B0C50B16234534F12305A5C068C2C11ACC3814943D842CF6B04FF0094B33010D07362D84D8742887A5F8DFC479CE67D4F9DAC1DA4C6FDC5617B4991ED02C7269C90ED3B803243AC2347609779E665C34F5B968EE57EB3B58166C68B772C267651E65D798707911B11E2D9B1E74E0C3BDFC2FA9D9F3B0ED62683768267A77AEFDE511EB72F8E161B1A663D64280EC604231752D9A58F2202E7CA3CCB261EDC393F33E72053ED31D48588DCF0D23D27997CF8EF71D1875B0C772743CC47B48C7E63B16C9D8B921FFAC587B1A311E06B8393D45CFF0093FA3FE2713834F693A66647A998A3B58F943B4877114EF71DA90981EB29CF7A6EF99FDE534461C0A686C686C5CB8D8D88167911F33779BE67FDDEC7890EE5EA6FBC3B0CC37A3920B33442EC236CE68A744CD98478253029E65D63D8E798C31460E6C73583A9731FCEF63A0711846250E8D21120701E014F178BA27731F30F98F7CD9E677BDA46272201188F111B91E2567358A79AE4831B3A8E58E1E991D5842D8841E41456314E868133BC7934D84E23451D4501EDBD87F361DA147061B253E97C315EFC1DF97BF11EB4218CF594647B93AC68FBE77AAF6947FF0066CD9D1793B14FD0769E1A8F7041EBC3E746B1E61ED2043B47BCA7B98FA4F580A01D47F07C1BCF848FF23E47F59B31EB3F2BD4D8B1F53F09F30C6C77BF3BCDA3FC51D4762E43B5FA18721EC1E47B6C762EB47794773E83CCF01E4ECC4847DD3D2F13A9A4FD4DCD4B1EE1A163B9A78BDEF794E8D9ED4E69B1C4A7CED8E4FA0EC3EDA3F79BBC5B1ED113D4F53A1C8EA2E30F70F79EF7F1BC1F70F8CA381F84B9ED3A9C1F998F6346CF03B0D4F88EE29E07EC6E723F3BC9E4F79DE7B47B4FC4F5BFE4798F0892CFDF28FA9FD459F99FB8C6C7D2FB41B3A1D6F71CCFE27E43DD7DF399A3FF00B7E17E7789B1DA7CE77BE8380C3EF977D4E8FCAFA8D43F31DE9669D1847E65FBC7C04698435789D6FC447561458D0B347223F0BC569A356C723F2068D3C0BB1347EEBC1B2BA0586E51085163EE972C362375B3AA5D8B4FC0D11A52CC028EB61F3B08C29A6308C2C6C452C51F788C2240D1844A7445834B0FBAD98460AD1752CC4B0C28B0373DC28A6112958964B372861453A3B3E96044880462244752091A74783E96118469C30A291E49444A28763D0DC58314CBA3C9A5D12980E81EDB08C21AA27368A28688C2E7B450C185883A3D6429B308C2303D434C2861A9C4BBB1106CBE960C2860C1D48771A912E51E72EC51836353B4B145140245BBDAC2250D23EE1723663B1D6412C330943E81A122308D8F7582510F53491225D87DB1A4A7EDB1B0941661F6DA4D4FB68D234D3E9741A6863E838A974F49B3A9EF88EAFA13F1A689EEBF208FDE20FBE242E53CCFD2FE06887E03DF3DE6241FC0F73C0D8F89EA3DE7E828A6C5CED7BCF43C9FDEF9DE0731F88F6CFD479CF0863E468F88F84B163EF27F47F93EB028A1F582137FFFA3000301F580A71EF1FB1ED3E43E33F01E0DCFD0FA1FCAFDD367E20D5FC0FDB7CCF79FF0F37E829FCEAC3CE7A4D9F75E0F1742CFE46E1C8D8A791F2285D8B4D14ECF20FBA5116EF22C1B3C4A5E4FB6DDD9D0296CB1763ADF6DD8BAD8D8EF2CFC81B3AB0A789EEBB1668A352E175E668F00F500144757810E6C6C6879D740A0CDDCC393A3B11D1EE21634636032D1B34C57A978A53DAE8B4B082D017C58F718C7A9D1B302808A1660536294D5A2C51F03008B4C08701816341D1853E761A3000B39D0E2732373DD6302C6008ECD363A9A6987B6BC152046CB0A0A0A7A8EB3679A0AB0A05EA5EA7DB7DA2822B0230A029A798408C399CCA5A0B05986CB4D9D1F80B05802302332D0469840D8D02C59D1D0E468109980E62E60597569E6BC0E07360598C0734DC346B3D811EE28E6C666E4CACC462D3A97765A69EF29A084CAC10B2C31199A0852DCE659D4ED38E204CD82F9B972EEA4CD9BB73439B7002D942C11853E9561D4BB1442CD9994230A5E21AB0D9F33C0B2C57314356E68D9EB283911D5B11801595811B144740D8211B2103ED2C2E141661A068D3169EB0EE3836400267428840A2162040D08AE868F023B3021A1658B0D5E0EC703911D5D08CCC6001B3A04282881468163A8BB45D8B9D8214726EBA0463A9DCF2C141331381C98118582CFBEC42EE4C40D5853A10A6318045E47323C8B983B9BB468D1F79844286C7363A040B3EEB029D8EC5A611698D8363479846E81169A7B9A68B0598C3D26C52C28A69EC7523021C0D48E81C18C08B4DCD1E2DD08D30F4B47586AF99A21AB1E6F123C5F6DEA610D8D8E468EA43A9B9674756346871755846E68F221D8713B1F305D3539BA118EA713B9EA7DB29A08DDF8CFB6E8ECFBA727E63B0FCC7FC1C4FF00C07A0F3AFA4F0843E6763B1FE2723C358F33EF1E6743D478341B1B3FA1FDCFE7391F51DA6AFEF792E853A3F95EA23707DD3E8781EEBEFBC57F6373C274D187C8FE9366CFC2FF00A3FEAFED781E0E6FC67C07E47E13F09FFD06CFA4FC47DC7FABFD43D621BCFAC08B1F09D3D62EFCFAC0C24FE8FAC06ACF58092BFF00C7D60508FAC05EDF0E2363B5FBEFF429FE878733FF0097EB3E57FA1F11E0E0FE17F49DE43DE7E95F81F9CF0697F2AFA4FF00B1F09A1F2947B8ADDF95A7DC0F09C7C3C9F5823A9EB2F497FE5FCAC391DC43F1AF361E663F205D61C5D8B30FC4732E118F16F887C2D8EA2EE8D2842CBA9EA3891A6CC2EF068D08ECFBA43894B4F229D9F80A6E76B1762377468295F3961A48C4A79BC069D1D8A7ED3C4ED6C14364A21F007B851490B05DBBE868D4F3161EC556EFB4771DA5DEC4F53F5BF29EF94E87E97B18798851D4F8459F13F84F74F53E72CECFED63F5BA1FE67F52EFE83C288F7887F37C1BCF5808CBFD1FE67D27AC0634F580A21FFB7D602BE27F37F8BFB5E4BFECC7F7147F23FD9FE0D9631FE05DBBFE4858D0FD6D1016C90F9CFB453FC8CB71FAD356CC1A0A23FA0BB4C10C07F989700E67E078A6A814040B1FA04A42C1FA868A2E005D13FC1D9D80859B14E8C3E74582C0239B0D8743F094D9858211850A241B1F2BA88B166002252FED020F5BF2B1D8B170A222363E96C4456341A8D2306EFCE588B4DC5EB7E94B3B172250EA9F41B11A0A228C44743E33A8A574163431A3E3389DADF376C9F2B634562B02259B08D1F33638108AD8A383A1F416211A23185CA3EA2CB18908D14C6147F830E253663187CC5CD4234AD10B8C6C469A747E30D9D035489F88D98BA36281B8C389F7DE46841E2F5BEF966E5C846ED986AFD6EA11D0B1F8DD828F3147D042C0C28346E9A27E4288DCE00B07F1B1A7622EAC2E68FE568A230BAD9868FD458D88B6688D3F5814B76053F9C2C59B82052FEA2C14149FA4B9FEEFF55B3FBDF582213EB014F3C241F0B03EFBC0A3C1ADFBC7866B67C1A8E27AC0A1CFFE3EA3FEE7AC0704F5809E9E156FFA1EB01103D603147FA9C1D1EF7E83ACB3E12AFEF389A3458FE27EB7FE9B1FACF4BF89B9E83F2365D1FDC706C75BFA5A3E17EEBA977A9A6C751EE1F114ECFA9B163E06CFC8D8D5FF00D3DADCF80A357F5173E23E078B4FB858FA1F53F84D4F514BF49ED34503FC9A4D1F913DC7ADF9DF688B1F89F7DD5BBFBDB8FCEFA98FAC04F0F89FB8FAC04FDF5A8A47FFA1"),
					taWithIsDg3Set,
					emptySet,
					emptySet);
			ePassAppl.addChild(epassDg3);
			
			//ePass DG4
			CardFile epassDg4 = new ElementaryFile(
					new FileIdentifier(0x0104),
					new ShortFileIdentifier(0x04),
					HexString.toByteArray("768233EA7F618233E50201027F60821946A1128002010181011082010187020101880200095F2E82192D49495200303130000000192D000001002D00000096001000A30095180000000000000000000000000000000000010001000133FFFFFFFF000018F20000000C6A5020200D0A870A00000014667479706A703220000000006A703220000000476A703268000000166968647200000095000000A30003070700000000000F636F6C72010000000000100000001A726573200000001272657363004800FE004800FE0404000000006A703263FF4FFF51002F0000000000A3000000950000000000000000000000A30000009500000000000000000003070101070101070101FF5C00174260C8480C480C4819406E406E40B4385338533817FF5D00180142606D57725772578A40174017405A47F947F94786FF5D0018024261AA48DC48DC48EA414841484194392A392A38E8FF52000C00000001010304040000FF64000F00014C57465F4A50325F323036FF90000A0000000017E20001FF93CFE7A2E004CAB4351E286BE36508D3D7182A695F34686E1CB1CAFAA583C34626556BB79665DE5AB5B4DA559B6B199813EDBA14F4E985964A2523D984EAC606D172800A27FD27FE7266949A000E20F245556FADD2A68F6F35DB112AAA02B17BAC87BBAB6B5A4D489BBE3A6A03712050F0F7C343C6DCF864842F1CB459C434D679E921DBB411D32047396786F5BC9D0E497654BFF77258B819890B046F8664C4C36B5A51DBB87406343772974F7CD22A56914CFF56F8C1DC52E45410E87A409CD32E4B2C775491B7E081FAB0DA4788F001AFCDA2A61C9293DA0B35F433FCEB6D0F14E180D1ABEA7145AC5AE382CC64849C47EE030C973D8A60DDFBB4083C15EAAF03B0314F3D31EC1CC8A9210B1A187427288134AB62BBA9EC4737C1062659C5F6CA878E1674E76A24C0E161FBC4A362F79BA9AB78162735EAFB562FC1A422D7E6AD01B9806A1E027D22795B785DFD3A889E96520521B733F4C40398F68E92FA0A9E59FB3D27F270312AD5FB28870929C2824E2AF89D341BFB62F1FB15AE89F01D202526CF34AED9A0697AD624EFFCCD4288E27320EA5AFE8488436C3A7381F04EF396D749E037CFCA0877930341C330D3A56C5117B62D7C61C8DA2320A945C73DFF0E9D7F7A1535A659632D8C05C781F2AFE30EDEAE583C71D7FEB33185D2A63C83DD015A277580483DC1AA534B2FCA06BBBD37A4E7A3BC456B0BC3AA11A4881930B323166044E9FBBCC47F79BB50E6087D55CBBB6969E4F16B14CB48E56307ABF82B79F1A6F0647C3E51B32CC51F7DDFB0F7C1F86CD01178AC92C972EC712FBAFCFB911ED175BF97061D76B55E0B2A696A80D28C6432F9FACEE179CE4D5126FDCF18C4A15B53958663D9087029E40F16A07A9E9E9EFB23FA22D412E3E387C0F1760A39262BD66B25478581C56B2C0C95E22890CF83396FF6CD537F326AB1D3EA7ADE2ECE8CB3A032CA6E059E2CD389081C48D63EF9C0B8C730E616F6B5FECE7A968FD737DFC3FDEF6480F9AA298DA5E43062484905A42B5ACBBB5DE0431BF8A4749D79445347466642DD268C3686822AC6613446EBDCFE5E664F8ED3B9F7D28AEDC8DBC3D1906FE42E6DD5AEC8A0B28A9048D4E36D3377E65468B481C57D88AFF23E5E3960E76CF4A802E230AE74A86EA034D5000BAF4858901C66DAEA65FDC5ED5C0D0762232AB928729EDE3556C6D822502221CD3B05E2B8EC08782F418DE5B353A2ED7DF32E833EAC829A27AB4D7DDA7D022A0EA72D4AF80CA67CCEE33A661BE260BD768EFDF28819A89AF247BE4FF744F4FD9DA0DBD8D5CF009F1656C399111360BC9C4BDC132B5DFF3B992C31EF521021C9FB9DDF799316168704A211F880A1DB5475DBAB55593B6E84EFAC0323C3F3D950180ACBDA5581A8088AC79921154DD265447476C39AD7337ECDDC61AA9084BD823989F9BF6D8FD04C054EA2B678BCFAECA72721B99D1AAEB4CD1234AA44FA63E69391921F3CF42F86CEEC3C0B266A5C2E03EA2DF5733F718774DC196FB6C431A8463DC926E54FFDB29D2BC3FA1AA0E5BA6E4FECCC051B6934E08D8729F2BB6A52C93FDD81215C651A2AD4B1329920173FFF7EA49FB88410D1DAA76426D46153B0519C89281A58E237EE7B770A5DFF5F26F47F440AD22E25653587FD13974EC45D1751753D35E4AAA5B471E4A0B05677A276364F29477C794BED6852A095EA1E4AB67DFC4F6A296D4BCF3D0D351909A934D7FAC64B3F80FDC43CA03173DDE1E3D201D63BAD760912698DF501A5D0901FD2CC270D8EC56D2BFB1415447F3B51FF2B15E1D9B3363C03F1AA20E9EBE2782C154F516DBB5E76A445E9AFD46B123DDC47B9C12DEB9E1DB3953B4B7F1B04BF4603C0933A17E7954CAE79351809CE84F2E017506F2CE16D4F51C6FDBDACCE4C262A7BBC4541242CEEF76FF755D58B165AFD854887274597D092B1437D1E95AA3CDBB9229793A3C3EABBE3EDD0983E7DA6822D2144A677B3B2BF74E2E1DC92D7CEAB1384D497ED1F2839CFE6A379F12DDEF28F2EA295F16F26598C611EE5974F9BEEC1ADBB627BBC587E8029A0489AE01499255ED651CD4FC8E874EB8B19704839C2C7072EABF511A394DB65DE952A05FDA3C56968DA508714C04E3F299D3B05BED852CF6ED74A996002D95F627E91303537BA12B58AEA75A3781297B1BA3A2EEDB38F4E4EBD35F8C917606DD375E5BD057C571E21F790F63D5D3489B93731196EE3655CA63A10DB6DF99ACE54A906391A2BDC1EC46807ADA003504E6FBD1EDAD7FF807D7FD17220B2CCDBB330E92C1A93E095B607E129E7505381693C2786EF8CA8901E16FC0540BDE3CE7B87ABC758F83B718C7CD5CD20A2E695DE27A8E6B825E2B6274AE29EF3C0A1D74995EE69E80FDB74CFB326865EAF49A5D571D0FA018C5F2A4E8F0B937A497C1D72B126ACE2793C75C09C77233313EAC3C2684C9BBAD1D75D9BDC2A4D13960BABDE90FF53C7676559E8B9D8E3CEF66C130B6D3CA6E95E2DD1F3CA08DBB40CE70912D877A20383EC70BADA605AC028589C9A431AAF22B3EC2FAED8E9CCF2B90EE979AC8CAA6A3D682A0AF9ADA4595B310993608B7B86C8BF43A25CD132DCDD99ADF1C69E283BE8D6483A888132BF529F4A1AAC07040D082F398C55294F0BD753B5CD8BC618849B50E7F9DBC960954713DED77EB888BF4BB65153F4BEAB802373A8C960E4B9C2F32546F5F33D749A01E9C2A97D078A57760EE65762723423F82F72356185E82A1694437E7B6F2ADBBD59BECC844732E606B9E6228B8CB40C9C0535B08B4D66A8F40C4B9CD4A8FDDFEC00C2DE44587E0C2007084E1FD31697BFF4DE5CD502B89BE2216A9F49C8FD44F2AC8DC75778BC23A2C324D7B41A8B827BC951F435DD4BC5E9866FFF270A9E9ACE2B8B13166134A890E19E0A359A176E7ECF61B02F168A723B717E357E3A6FF116559D161138BA916B60099B9D6D7DF95160FF701966329A0547B16C2EE96C0DBA75458FC03E1DDA01F0EED0076980174D5CAED8064BB84B1AE546C84F853A808B482E26165C233525E0E439FB808D756699B22BD8F647E29309324120E189267115D5FC1E1A883A880E71C7541F4036535800FA3715E0860F757EFEF47F3FB3D878E4D85C38DEB28F66869B8B140DE15CB8911DC0258D23DBFBEFE2A423D60B62A6FDA5170F4B2531BE2968702457327FE9A6C08B355DE5DA66711FD4D9FE971B54554ED453F69041133E54CCF76EB3054A9F2AE1E5E6D0458848D89A548316C601AD774625B08F19BB3402CD8BAA9F64E92B8DB9C49BB14DACEAC6F2A21E1151E927ACAA245A954BF1F768760AC35806ACEBB7E7AD3C87CE7ABF589D27E96DACCEAADEA6F7275D1978266D1694CAEA702F5A2CCBFCC8343901F89C10044C2977C07C3A7C07C3AC403AA81801E7107EF6EED9F73C59195DBABC47560D495642A23188A8836DAA806BFB1E61FAEA241DF5D6DDF9FD8D4737D428B89CC8EF3A183BB45A7BCD5193B652C9A4254725F97C1D59D4C4271BB7FB057F49ECF5812BA6ABB6ED514C2AE00CC87769D277DF5556B2CF310DEACDA9EF37DA0EDC1ADCA61CAA3D29AE52121886B8510369129C5A3E110C3EAFBFABFB81345F307B58F02160D4E31AC96FE8C6D0B04FF0523377DB4FCCAF61A392197A88F0B5E11F9F3001F40AB6546466FF5DC3E7F46F1F574D307C9D65DCB3D0DE7D3418E673F0BB0E77407A065D9B1E1E989E8D6C8DCC8F23B34E797F8493499EAF2193B7D6F350D1D061C8C2B84BD3BE8338C09DB42C01AA27736A16A53ED2EF634D93D7DC5227A14A5E62F32A4EE647CD2217816B500BBE36B3FE218304F437238C537EB5AA0F75E00B66DA0B946E65F85445A1B02470F2D8DC15374238BDF691FA0798B703F1218582F94E8893037BEAC689C173895666CC57AC5979493BEE31FABE1E39BC525537D6603404FE99A76E159119DCD58B4C97E845E682997B2EE073E70C35D35DAD53E0A41E94181EB7F98A23FB0D2EA887E7CA159443F51027532F296D08F9B401F90940CBBDBDAD2EF55EF189C49D5599F803B179C356A4FCCE200BF99E3C614389EDC2648E900D00D853870AEC88D5046D747ADCAD3FD046C47A9E2E0F79313F5AF4E64F640FBB446BDA49550AF476402A3713F0F53820905936D871393A1D1C127AC8FAE5C92ED2226DA8D75B89AB56D07A0BACEBBA271FA8DFEA6B6660F1DA647B55748B21A4AD9890770C8C2F96A865C58CB4B1132682A9006B5C301BA4DBD325C352FB82EABE38E90C00B7290FB41E39ED9571EB5117D215E2C48CDB6F84A48378C87D87CD1B33DE25F2C49FE0164382648E8F86B9046040D23926E5109A93645BC6DA984B2ACBB7B0ABA46AB9A74A878D69924DED1BD6664002A6714C644F6E68A2CEF2E5B0B21637AC69DF510CE0BB5D15D0A76288195025099F07DD53F7433FAACD1B0336CB15375AFDACD45386AE4CC0470D46AE0DB14FF1310FF08BFD52599382EF71C55067B7E9679416241B9AF3CE714F0CF51C0C75407F442FCC6BD70D2E739D2E42CDDBBF09586261A626E8A3F4E6F83A9399D809D5C76C7AA87E5EA5410D620610754CF378D133EA4461B6953A4C0594B2114ADBE36ADB5F563230C492F94CE84E9D495176C99ACA47879B911230C957277BA267D9FE1FA59596D3CB4E3BEE5B2F9528C0AFAF6B7F4B17F278DD0AA5F28E527F06DA3747DBDB510B9682074A77931A25A2E9FAE42546D8B6D91B51F2ECB98DC271A7B46912E406402D380F1358784706706DBF40F0BB65E4FC43D97BFAA30E3AE98D0CBBF336493BC36D6E115E51C0ED3DFCBDAED8EF2E98D29625AD7A6B185E37260709519A8C276A080DB5CB955F07627D6FF749A8B9BDD4E6351371B446D458514296C18AFF01DE9D61EE5663BDAB5BB1473F57597A2E02E38A0E3120045FBE7F705864C132FB6ED539963A2618D8152C94B5C8C0CFB6F2D3E272E570351C303BA99E2B709B76A6CD7A4BCC44018789C78161CE3C7537AF59ED076A50DA4B1AC61CD150B6C4412294850289967E4D06A29F6FBF9CFA4B642EBCADDBEC4B0A2BB7FA450C2005A30ECC69E38E143A194EBDE202A2191D83BBB541F4F5153B4E69F388F52E34F2D05B3CB44BDDE0573BFBF99A2173ED0C916E1C9477C7A3239B58D74AADE4BF5F49D5BB57F324A8669F3DDFB8F155B5D2CF9327F02EF017D7576B16DEFF3F1B3C4B2F2DBF3B9B7234A62CF97EC88545A98F988A3C3963633E8903D5B786C358FF79CCBDE825DB2703F3355EC4402320995B0DDFDC3C10A83DB3D049BBB612CAFDD0594384A7B2B178FE926B3AD69D84278F254143624F0BEFB99910B77B5B7536E5374C757461B21D5A4A390EAE239147DA63F7D1D610BBFED03879935AB448F465C3237EE1C30C7071DECEFEC3C37EAC533382563DD4A57EAD772FF210A467B7443015980E5A00644589E0B6ECA97909447BDA3CC759CAED1D18EFA4D37B4EB8780E26842940B082FC00432556AFB31B8F012C1C5398F27C1EC4E265EAF518788E914398FAE1D443AE741D896B9FE36F3464D101B4B064D5268F934AF5C9C0963B3C6F303D6623425E2F7AA49D19CABD9026F190493373C8E2F6EBDB5A0B329B3CAC32BB9BD47DEC363D1128C12CC7DA5B2D8BB5E7B7B5A89B3FA8A959ACF81781EB3531026FE21F6505BE9BF025C700B512B3501AB1473F521B7E2E062C0F7A9C78C20362A2750D4F922C07BA2FF42A87F9B53F6DFF42A6E6E414832F0DF8CE4893CDDD6F84DCA73C9D13CFF1C169A6FE20443E6089F3025F638B40B275246E98DC1090D544B69A806AE00767016FE2D09F2600951AB8DAB2D67C57431EAC07C900D89B2605DE336DB858795459E01ECFF9EA072B2E27D3C0775E81F0EB700900219E02C93F43BC0B5E3B735CF5641B099FE4EB455502D59B7FAFAA49EA9114568BDD71D7D57575D79001A4668F36104896A78F7CDCC5330BB44B22E2453DC2B84A3F00A8939383198662A712530CAFB63D65E2036C07DAC6D9AFD6BA413D8756A179F91473AB0D4B97BB8BE09F49A5A98F0A28CD4172BFDB03F9E3EE7101104C6222E5B2CECDBA55B1BC0F915E8C7EF663B5E8F4A49FBB64388E4EF59701C03D10BD0502257F91F81AD2CE222B5717C9E0987623689D8D0C2FC69DC5F2C076BA03B620224C4D0C15520411835C7162051C621F9F6E17B9595E76FBAF2C51DCF96C18768B978336005FA141892431F82B97B73D1C47B1DA8AAB15FE6054B71179958366D3B6092BF4E45009AA9E5AC50FF2A6BE447E69A1A60AD7305FC58399EF0937E1F93BAF7C3A3DF0ED913E1F93D025F0EC5F9353CEA182EECD2D2FC344F0D38B646338D3D6765E709586B6F0615B968CAEC581FCBC123F257471586468ACF52836A73422B1DBBD79C8E786C8F5C83D3EB2EF40E2788B9F6F2BDC0771BE353DAFC883651115EE08C91CBB1983BA0A547619D35C03746870BB2433CBF16759917D77BA3C648E26D22E0F812AEEBDDEB650FF0C4979123151202481A10EA17E032ECAAE308536904420B528F5E60B30D07FA185A7F0224602889E313D147584E9A0EEEE95DC87BD7157F4681D8FCC7E5C46231A621755F02DC912D94AD6ABDB195DBA59F98BD66FB6456244903C2481CBAE5F10ECDB6671FC2ABB815F251968680D1913A7E3B4919F6934E3DA22DAAF1197754862ACDFEEAA25428BEC4BA620EEC2EF08EBC9D3BEBC99B8A19E127A9E5B9EAB99752E2F58B7566B69E5746FF2DDE901438F44D72A1CC89C78289583F449FB79F6BE6FD81C6893EDA2AF11EF81DCD6DF62EEF54E63DDCC166552680B89715FDFD70C11522AE091E3D522975A8E8FB30FD9CECC38C4E083F2702CB8E0433E8E7DAC42531AC50E5C118862BB7DC0325A17F9B602B8D1830F76CABAD39FEB38BEC93D5DD492AF03B87A4A92A21294947B79801864A07B2FC1E6EBCEE6E5BC67F25FE3059B08A4833B3BF2526677F1557C740B661F86845A69866E55C73FA8DC024891F21FD0B99FE2306C19A51E3EB731A38554F92FC9B49F6242643485319F8A722B9F8FD7A61670A31FE3135B5FF19E606F740C45BFC0B3EAFECA9960F5747C12CDCEBBB6DD149EC4BBF374543757B5BE1BF027211ECBAB506CE346345ABED60DDBEAAB91228B93C7B4DE07D608CAB7FF76D7E0CC69226B73C5F0625B18E7B16D99592FDB3B3FCEA2FB58EB614EEB27B4D787FBFF2ACB83652EB8B62CEB6AD73947A34B9F0D61D237197EAAC8E6015072838C3F4B2C0523A09955F03A8FF0F860CA3B61B9DBEC83B023CE123BE48C95D561471B3371EDD9F84796E39A785A7EA03D03CCF98DA7299BB4F07768AFF7957DADA5D34629C602CCAEC27FC7D9D972ACBA0A3083CD1459C887FAF7508FC63866B88C05684445DDF025288A786E1F42A3F9D7E9A59C6F51469B1C588C6B9D4E2F320862F0076287EAC64AEF76FD4DC3B26AE06C0B49F027F58EBE14FD6B92262C3E9BF79E6C8001E5B27765815B03A11923CAEA9AE094C5BDCF8825E41774912453233F2CF9507E7282A603DBB8A3727BE7E46C4D682E77E80FF6B227CE9B4BBF543F2F2993BCEA500ED847B3206904C8B64749B8836C7365662F0D081019E44506EF25EB0FAE997E26838978F8204CB6794EB7FCB9E81A1C5290148A053E29719FAE9B23304620A9D4132A5CC0BD1432C2074469665E8CF371483A80CFBF8B9B3958A35CDD3F862EC7279A162E5ACF4A994F0DDA030D86C2874A4FFD50855799B5238004E2D167146E548B08F7A756A6DC10E64E287ABD9C97D444C49D472FC5A18E6B78AC3D1E97CB6FA2EA445CE99F6E45A0DFCEF803844C26887D68A66F9BD338A1486430902DBB13EB4CED6B1FBEFBEE308A0086048F4E753F08BCFD4788DC78E752DD437DCEBD6402FB1D0E19D4F3EBF3305D268F89AF8498040280850A8E14876312DC64C7625D0E18C53C355615FBFF5BDC2E9C2667916C6938B9946ADDACB06636DE051C9777C79FF3E5A3FCB15365F4A3163FEE0B862013699F045BF711B4ED765983911AD34F1F36CCFB9A26755B496B5AE4C925047BA556E1D22E25086BF636030D9D15E328680F333272D16961FD04EFD8411A9A1CE4B09A4404DC15429D136209BF84170055A102609E621607B2DF20D5DBBA9EB9ABB4F3B891CC650984F3C7749BE003D045A37B81C9B1D07023D318C67EE54AE67AF59C47AFADBF1900D1EB3E4BCF46F1FA935875AFBDD067A9415FC3F10EA97E688FFBF587367C83D4B12A73A3957C01D4A662A9C0967B7E6B995D6E45BD9F7CB392D78DC456F762CA4B722A7E5B75C731506A8F8400E1E78BC3149E5014C0CB0FEA3B9C108583549FC813544A0C916A2E263152B58DC749B3A7EF4D55E5A3806C638B9895F682715D849146CE239FDB69472C727E7A575FAAA54058B10077EF3F8D6A5AF56ECFBDD8743401AA0696F711844B38BB2A9B561D33E050DCD429CE57E25EA3FEC70B277C9B1C6CEA9C2A2C38545FACB0BD52B7EA7F7C949A3498C01B3A5D55F61A05E8B95D247ACA51DBC7BB0D4A183F4C37FE84835B2B7C93C38B982CAF0A8AB737C60FFF30D8D451356CDD91EB8D7CE27EF2623A792B329A341ADFFBC059629E986BE3E01946028DE860F0CF5CD89BF0D445C6986D80FFD97F60821A92A1128002010181011082010287020101880200095F2E821A79494952003031300000001A79000001002D00000096001000A00098180000000000000000000000000000000000020001000133FFFFFFFF00001A3E0000000C6A5020200D0A870A00000014667479706A703220000000006A7032200000002D6A703268000000166968647200000098000000A00003070700000000000F636F6C7201000000000010000000006A703263FF4FFF51002F0000000000A0000000980000000000000000000000A00000009800000000000000000003070101070101070101FF5C00174260C8480C480C4819406E406E40B4385338533817FF5D00180142606D57725772578A40174017405A47F947F94786FF5D0018024261AA48DC48DC48EA414841484194392A392A38E8FF52000C00000001010304040000FF64000F00014C57465F4A50325F323036FF90000A0000000019480001FF93CFE7A10008DDE2DE9D8E069FCA4D0AC449C8005819CA79F0AB67834BF794DBB2622BD00FCB2BF63AD19F3E7122AC235AF26D72C40DFC4C633ADCD53021553EDDF8DE97F9E52EAAAA0A248E362AF57563697563C630971AF964B0A840BF9898B629CFEEA13D3506953A6C9854888B129BE2DDF429FACB1E1D3F4B7B7CACB68D6725240322EE94B0C35E323F8379534288FF0D7B0AB668B48281EAA8D1B121864526BADE94655C3348E77505BCCCECFF3A5A4C4FF7E12A5DD8F006A59A1D5CFF798CC569E3F37B20E5F1184C67DE38B3A195E90BE9830CF773F37407344B396191C6955226CF2CBE6EF65C240570BB494396B1946F806965432A2B1A7E099943D115E9652EAB673481B2F7EC07EEB696647B327103A67D2B81165255F6C6F26ECA70A6AAD3F96634D04B11268D4F7A194EED73A1C2320B1FE0F0F277962B8755974FE462677D6507C5B51C9C76EBE607F7DE8829BBD6993B295A2894D3AFD376A5C6117BCFCE0D4BDFF7A5BCC22C03D7EB30124F8436DAFD2741399FDF729CF07075886638C3911FC44B9A7CA7C828A6ACDDD132499AC3626852D6D2C56C539272EC05046AC826674DADD856C2C6CDE45E999334E03F1586051AF6FB6A1DC0A63E2149B13A34BFBEF4DF0D7B1383AD395DB6781D948A677AE85FC3DCAA98DC79D89B075CA2A17F94CD471E49E63391E907EC26737F5F64A71ACB6F16E97A3B3462C700F211781DD3EA902E784FC0E31CCBB1D9C72FC3F3D8D0543D5EE6654C18A259B7409A9D85B014D8C77D0CEB1297C36F23FB1429E08B00E06E02E72A0F85A532E6987DD5E8B2C3640EBD89DEAABACC7A367AA818F90BD369A30D7113024AA3FF541F139071F95A2EB854C07155E4E87DD9AFC12EC073B68797B058036D3D122408277A70D4D79DCBC3EDF6B8C61BF572EA4C919E76FB565F804C764E09E51DFF304B7C966E1BD91E82B75424C08FC5AE4426791778D3DF140039E77B85991992672031612AE76D79F96A5D55F957A6A9F157C2D1FA0EBEDDE6077DACC7AF8C9E371E6D1BCFBA947D7D3D2ABE850E121DDDD7441DFE3AE48D5B26001AEE7F726EFD6DEEA264D2C20352D5531A0F49F772D9D5102A5C95E15E49AA461C0161E74C3C07C138832D496F944C864DEC8E3DF56E6FF0F17FB322A311C9A6D38ED79B63CB7A9A528D1CA0B5C3C821E7146626DFE26AB1C085E9EAFF1322BA33BBB5549868001F9F1A19030363E44FEDF31F8BEF048D70C6999874236E522A0B0D9F9816F98555EB9E2470BB14E188189B6C565670B3DF6ED653F272FD5AD324813322CC75236F11C3F3D87039EAF3422F3132783A3AEC215461A948EC08C574BB3B655B42E5A04BE5628017D852CDE0C9FBDE06B7F57CD6C0E8EA32CBDBF8BD453496DFEA55B3AAD256BB937107CBA7E607784A60C8F60AAB21EFCB7B3F3DF04900715A3FD6A36D53F356FC5B5D47287647A6579F6D6556E034600EFB1B23258424733DB86146F1519A62D6E502E3D4550656C0C700092B8C101C6736F6EE708AE0670C2DDA9A411D4E6B2083AEC735D561190953D84BE89C605D4479170F5D170ED4374DBDA593FB8F2E5CADB82E2D947D80F269BE97B517C422B0161BEF451B3D51321EF11BB16062853965C50108D6C7EC0078EA653FEFCA4B3E239D3ABBCB07A645FB84DAB7D14F456515BCB4598143A8F02AC0F93C00B2A35E35FB7008FF3C45101780C226DC68DC9B305D3284FEDEB14D7F2E3CA37B253012D7F658087AF14C38ED7E01BD928C28D2F3FF1B1FA486C4C3026848E374C5B83679579654FCD5AC2686A7AC4EE5AC907068B94196413F4DCCC3A978A19B0D2B7A34D6A63AE4AEAD8EA33582788F10BC5DE78B46630E60DFC3EABCA3EDBEA0F9F6489222B804390BB6F8AA4FF1B3E4E8EF3596D030B659091D1E5AC50FADD01CD24C436C9D8D0243B17741E95193C140F62054A3D15D37C89F0A98B4D7B6CA2C50645DA118D7B0B265EF849A722CF1B03E2BB10339A5B8077973ECC1FBB0C16CF63531B414E6D59FF990313FC62500F25BBDAD72E91A262C907821DDB3D8671B62D98FFEDF10A005C0DAA7F5EEF2C8907EBEE3DC16DBC9AC3CDE060A8D6E590A5F5428FC50D58F19C0CE57BB5831F4021444E3E4F1CCB9BCF75BDE5EAEEC51ABE76B79D6B0209373B8F33CB08488DC34108EB0728E515EA5C1DC4A727AFE4498B687E1C138A7D70B0CA0BC9A1C00E6E41248678FD548232782375990387CD3D9691EEE57285FC107547E882C11FAE0C57E02A4F0BB0EE4D60C17C3C1CDE28BC748F0EC8115D0BB2CFA26BAD99F50DADDF511E1135B8AEF9DCD1022B63422CB5800B3D5026FC09B2013D8CDB20E55FF72A7DA358D3ACDFF5F0ED57DF4A19869B752628EEF2A3C4E67BD3710705D9BBEDBE66AE1600A015A453FAF7933E6302B1A618E8EB280757AA4916FC1974B0799743A274F56ACD62F104E90F1AA5D656BE822D19009091FED4AA40A86C55E011D870A870C3B04F53566831D00B0CB694C364EDBA707CF0599B2F1A3ABAD3F39002CF2B549CC7BAAC99CA6CEDE3C2C01AE9AE041C774BADD06464141C793DE5F8A7F15CB7033F6050AB7316AA81B8D531A5755D9556D88425C44BF88E5C33D2995DC3B3C6A4CB6BA73F64BDD530BC96CC49002D5996D149FA8BD0AD40F18E3E6CC74CAE8B1B69D230F0FA11ADF7EB76BE06EBC8978EFE4A1173AA27EDC0A91B3B6AEE2748EDF090C818E1572984119E31B4071E55EC506E30FCC2FF3002AE41B6A01EA42CCDFEBA08A7B72CBD77F40781E273A6624E245CE2C6E3E59844074F1280F9FF6C45A0B68775BAE211DE432D3024433F863E8AA2705CF773359E716041383A4CAC8B1271BA7C03E1D8203E4BB401DA07441D11879234BDF652E794143671AF1C9DFB22B3E0129002B23351907C13E83257101D0C404C34425891EBC674397D5895F874530C28CB5A49D25A8FB5186C9DE673B2DFE037DEB699B4B3E4BCD8D06AE50BA273CE84B58ADBF476054456F2F104F8670FA3CC392930038720CC3C3105A2B5C37ACF53710A2FB78D6AB06B4FF68BEFBB6E4E3EFFC841ED45BA2952379AD4FD9B3EA77C614BF271714C1F46064BC8B10366EE19F2D98A7B9557D263AB348E927963A713CE2435649DC121D1574CD41ADCBD7E66A17E9AFFA7801109D5424C431036FAE493E8881D2DE4BB139113634F91F598F092650D091A891454642B2B8D231319BC07C37703E4A9C01527338F5D62FD38755A4D8FF6285BCD2A561F42A06AF5E0E3989254B5CEE929893C3F3E9550B6A10FAB428C9CAF97F27E7362852C75E4A5B5614C99FCD4C12FA12FD13205736177855CC25A4E3D9DEDA3B7D6163B2E7468BC9E2250A3C36CB66165972492DA28EFEA5DC958D916FCF2FC02260E6ED40EEC4504D43AFBF3298F00982E4739C8CB36A336392C7315D3F7281360C082CBCD82FC3E7F4531F574ED0F9FB1ED8FACA55BBFA789170B0DB6E683E730056DD96477D356057ABA2235D35CC2CF5643A1BFBC3B36D12F119F9C60493DEDA90966FBD2F0AA03BA1F3D7E52B7C732A156022617B06CB9F9009E51928347B5E0E3833EE7E6C1CD04742104372BDBD3369E66566199E79002E708BC4C372CD7E03F81578F82492B5A5AC6EB28CB1F290F86273C7C854C7A3DD7E9DB65C93E500C8D75452A257554F58817B86DD65F81DBA55EF617749F8964B0AE20B133505F9C872FF155AD04BBDAE6E64CC442E0403786A18816648DA2E20D1633BE65AA4701366ABCA45896C590F803A8D262B629B69A286D0A560E7D485A1557E73454E68D310C8B09E6E002649047B1F5805B1DA4067B0C8C3F751CB315CA45AC973ADE511F8168B54B4B90FB329C581AA72030CE24F16C6CAE3B54CA83FDAC6479C8A1F18691F72B062992C2257B485B07C308E277B583BF379F4AF67A355A00953CB8BDEC6694D5AC4CAEDA319CA5AA771819CB2D7C3CE8ABB3E50A1208D39304D3D20F2AE8F59AEC0C20E6D0C3E180E19C715E26D7F10E5225410FC06691535050BD5F8FC9CDCF803DBB1821D519917006FAA5713F6A1ECE248FCCB79A426592BACC6EEDBE63131B4EEA06E0C5FDF005A11FA5A7E516F888D365084F3B7B3E504A066DC2A1BD76594CAFEAC299142CC70A4C291B53585A913220459EAB9BF6F87C344C7A25964D478E6A9A2DDED5D448F3E50F852B414263D7188C7A4CACB0108AD747D425C038BE9F62AD750ED0C89412F3D1BBB920DB6BE51314FE989146F5D3B34A66C80920AB9A2B62A95048C308BEE8C93E98E0360B66F300D2EBDB98C918424A18A2FA536DC5D83A6A0B0C007047DDB6F7E07F40E54E9DEFDC9EF3DA7EBE74E756EE37ED797AB66D6815BD0F9AFA1FCE417B95FD6E1A25080D4E1761427400D8212684A9B1F47725F55DA85DB819C8D336A5431F45EB979D292B07247E7BE4EEDC6CE2D0435B30884AE442D1AD693DD02712149898002D336FA717DBB68CD15CA2936D29C8607E6893F642B0105C04F9DB7328F93C7B81D6BE0F71E39766752FBBD0E3237497E18C50EFA0F7612795179D2098D659AFB14524BF83C0FC07AA217B01C7996F1039623CFC1C5CD344FCBC1844CA83C88FA3664E2051C2CD3C6BFC97A210509CD6A74D8C667D323F28B663FF5F9488E2AE2D45531BFE1F1200F077DC5738E538FE43A7BA9CCCC9C1ED0D2A24EC4AA67663F6A12C213F3F6B045BDC1460AAB64A1845C35BB86232764B4141C2B2242A38533522D014EB98006E4D627BFEFCBCEB125517144B1BC855AE940DE74E8621D8EE1DE8FDE109827A304F1964FA53B5A8B5C4453763B38D1CA7B9C6AB1BCA673A9EC74A5CE10A381A81D99C606949DF9024A0463BFD05F198DEE154354C30DEBEBD0BDDB63BA3B85531C3D69D5CB52B3F6D17C59B2172E415007B4E7E73303B0D08D6026789D07AA7C57F2370960538D5082D63215D4677C45357CEAD60DCF00DE2094329EDABC7C660DBF6455ADB4401BA0768021D99DEFDF366FB12754F494228E34C7BE040BAC71B6A92F973245E8E57EB110D0D967E9C5577A40C9932404F9BB29A59306EC328CA6979F4EE197AF5E79BD6996622D89D35C85D58EC2E65C3BBD121576B1C0DC5951579306EA78322DB3560E0F3E76CECC7BF45BD5F0C6E6296453882CA1C390BC58026EB820164DF7C928A4F10D69BB6DDA27BE7294FE2183E888FA659A6DC96D5EE4B732FBD4E6A298822336AD6C568D6DC1F47650F25B64F64020A89127448114F3ABB323A0AFE48931A5ED311D21B346CEF769A307947C2CDFF3A67CC7CEB64C50914CA3D6D8F1D0EC295720A9734B00B707AD050B968611A7D7AD237F30E3D5285F0669CCF3AE20F94769C26F32E78312F211F1EA94E274EA76E6AB39162A0CBEC8419690196E9716D76A880E9BB10375B7AD3398F6FA132A5B8DF8FB46C2E20B13F50FF1AA29C46A6AB0E52EDEF2164B76A52BCE22B4AEA08FDB07F14B8D3AF39E7D68A3187A4636D51548C3C0E8F723F3B4824A4B3FEFB268FA6C402DF4A2D79BE80A508A6EA954499C1D81C8E8C2AEABDBD07F71139CAAF88BC8261A7F3CA35758A7C2B8528A1B8BC028F440EC220D8C4BCBD64851FF62C4DF6745AAEC754C3BA34220B27F7E7EE6E807B84E100984426782FECAEDC2D6F5514306A9E2CD976384A16FB6209C046D718AE64DF2C6E17181B960EAA2586945851BBD2359C076BA03BA70C941C48624DC4C62E5E442E0CF45BD80F14788010EBD07FF74071736A8E2DA3A18486B743AB2B4112A6AAEA24C9F09B7941ADC5D1E2EB052BC4B7A42069BA9BE803337B852921D3E6247FCABDD54DF8B800491960D2135A5318B4633ABCACD49802EE35024E61EB8F54F4D4AA832B1BE5A8CF5644742DAD5A32E49A7C02A203B4AC95216A8E3851A8FE946B80C42123E4590DF9A57F6B3F81E44A37C1C122FB0B4F7666F9F26A9DA3A61788E050FE3F3FA89BE4D1D3E1DEAF91161F93D0A5F0F53FE4D4FF928785F0F5ACEDC4EDA7E4360F1B65CE500ADBCC772A45816953C467E6498D443487C3C144BE91C3E560219DB9E5B68135FFEB698C75D7A5D045E8B069C5D9E412CC7A4E56F161C5EFCCE1650FAE6105266BFF5A46E3497AD9454BAFCB91A69F374E8816427DAF4EFFC7FFF5E9818CCDCE96383B168D069F889C324022ED0C0DE85E61EC0FD3582163AC46688D918C422D7DA2AF923C2758AA05DEE27744587F642FEE34976909F09B48437B839499D245A5536680CACBC4596F85E3EC09F4CD8A50F86C7B3FE5D302D8F10AECBB8487B4CEA9AF830446C8EAF7E07441256162CCDB626FB80683C1B356D2806060E457F6193BD70BF7207BD2F08AB36ED8B7F1985CC903672F154C634EB91076810E7BD57860D011464E1350603AC093D04B215E87F4460C952A99B11428AE2977E7A35BBC56601C27A9BB9E74ED1CC65AE2D866741DB648483A65D4CD0E20AF244FE237E95A2E713E93EFE0EB46E16578CB7FA53075828E12738CCBA2F783522791C630ACCEB35413BE82F257C3C1FD20BC61815921CEA84A74B8553B9E40B9635B3ABE2AA8D2E96A8A72F9C0DE6850C1173F65F12CDC0387B9F7C54D8BBD7C14D58BCED512A082A03ED8F72AE810658E1FA51DC009546E99DA70D9FB76777CE4D21CD1964C3F8020B643E25DFBC454FF45EDC773D261B15A01A122DEC01BD9796CCB64C66D4A77DF4C93B84E1DD50D477C7808F1850DA644329DF7E1BBE7CC8988C6D928AB780FA673B8857C26417A717A48A6E628881ABC1407B28DFA9FF3701628BFD90032BF2364366F779B74043ABA91D43AC7B73D57A7E91EE33E949F5D5417D410C8A8782420F6BECC9E2471E13BEB545DF3A41451F3C194658B46767A16453F48ACC62BF77F05592D88E6EBB36947FB119E1640061C8B4C6DE135CA2995C0CA3990B1B93DE710E5F198DE5700E1AFF80B177BD816A2DE0F2ABF71036CD49894E733C394AB08FA67AD350DEA205D2EF547FD434CEEC6B2299BD0406244BB7EEF9F80D9CCC1DD93ACD4EE5E6F1805382241A9F8AF3B258E0A73B5FD3F73CA5F15EBEABD5B42B79BE83F82D1676EEE850EB14C77919AA2F79298C1FEC5C96D1A107C64DCC886E5555E4B30A550607145D99EF4F8F55C6AEADC68EDFA9A0836381CD38EBE5714F2FB4B6262D0269D72B4B51145C7ED11CB06ECD8FA7AFAF9AB8E31343E568BECD1165CD81AF627ADBB47D075C288059C2ECA4BE553988B8021A2F9B3E9C6A416025A16A0AFBD767AC6F7306E1D99025D4EEC11D6110DD6F11BE01684AB0BD120504A7A9101E897E9C98558E0F472FD1AFE0B1976CEEFCED21CD6CE866777F5FFA5328D3C71EB77CE695F0A2B493BAADDC67381E9C688F233A912EA614B612E6D17D8FC0E6CABFAD98EAF59D381DA6EC438056A63BB7B0FC76295064169BC18F544A451CB5A92B8D08EA6AEABA347D57702580003A1782C80B7E501B7334B076614A9E2BF344FB23FF4D2FA2A9D05A5B0A1270807E38E8E952EF07E76F89F609B21C283615CA30238C22A4643D21DD9F8935AA461B34B7356AABB16863D67FC46B96BFD747C26D673C7B759D635C49109926C996C6C26F4B157CCB02E94B6130D82194A4A77917214C3E84BBB7243D72FCCA703FEC5F40F040E3AF438CEE91609A59736546E4A5F53BE44325A0813EC0F5D8CFFF302F5776A4B85CEDDE656E1B9C102FFD88E29091249C87D1EB16C07A672CDB8E461BA0DB5DD7E06692D3BA9B233578192CE66A5A987805E837A572156119D8C72D152F8E21F1E549B250D1B22A24EF545D9C3B4AF776B5FD71293559CBD7AFE21AF8493DDEB3CC7E5CA55065E5EEBC37F5C2C51FF3FA2220869BF17B24883C50CC62C4A2D04B6879B13D5582416B8ACA182A12D80EE4CAFBF9A3B77395E61A569B46AEE03F2CD4653BDC8C7BE4B0F397208CC9C34DEDA06101A2812659FBF89FC144A52068FC84758344B530919A64A122A8559C54BC3D40980A81D8040124E080F1936D51C7E1DF8CB21377DEEA504F1B9BCAC1E8DECCAF6F3AFC784990DD0C0F6AC934BA81BC03657CB8840C3C8A90CF856B9A340BD1397A289E9298A00A167CC39C5896E5B5F22097AEA32EE60F23942E440AFF4C4CE3AE8AF34643DC197C6212A55C161B4FE077A03023614313D5CDDB5998E487A1126EF90A67F30708369664C57FD30A2F201B04022668CB052BC1815D9E142E94BB15566E0B2E7FF76008CCAF0799AB28271639690D545BEB626B289B2F86902532AB948B0E08EF31650D9DA995D1A149106F4AEB28B2ECA09DF1792CDA1CEAE9ACCF70754D906C945E04E95A4C53B6D947B5B6A287FF1DE1C3B7C503340D89141864A4063C6254EAA81F5DBE11BE2E2E27D24B34A190D37D4C53A3BAFDE0A69EB9170B1A5E3B39948EE982C5A6780A2FB9633BDCCD8B0C90E0F25F406CBA12A8995E2433527E34CD70944751484C2DEA18F37CFC3058F4B4D5E0664B636029F20F3D2D637C7751D31E8FF1EAA5406885C681622ED00FA9EF414155C3918E8A50AB78C461A6B13BC9C45B850F0739D513831789686DD66370AA8E23B6BD4913432F82FBBA74207E33B5576367B13FD0F6284ABE76B0BB47320C09C90B5A49CB7F00216DC3BCD7FDF2C6040DE6DAAE7E84CE70D4340194F71BD1DF3465A9D672DD4C4694F1EE46D26057471A08B38CCDC71C1FA9CEB3A71029C60F87D3C308CF763216F14F60E75031A3EC70D91056BBC276B38EE5B8CD3C4246F87AD16FB7BAB89C00326E48334593D26DD17634F459B0C194D9FDC23A7E194E3464D61340D85BB7816B0E7D37A821FE61874CBFBBF48049911A69EEF6920A70188BC5EBC3D66EA2BBDB1080683D62F02A3E96858B3DA2268B1F7844F3789F3A352968B0F2FC444D623EE768D2ADD907F8146BB5DCE30448E26C578CF8DEA577C9D089C2650795CD62E3C02CDB9439C91883BA799E7989777174377C6E932D4BFBF37C23B61049DF9303B1EB73A25FB36156E33D033019D68FD980FD795897CF0952A9E8E3E744E0B5C621BF8377CDBA32E56E04278615572BB8728790C8B947C713E2E6B3952EAD551B8BC89B880CD1FB423F1774A7EEA82EAA3EA268AA132DA7C48B32FA32F3A40318D62BD780FFD9"),
					taWithIsDg4Set,
					emptySet,
					emptySet);
			ePassAppl.addChild(epassDg4);
			
			return mf;
			
			
		} catch ( CertificateNotParseableException | NoSuchAlgorithmException | NoSuchProviderException | IOException e) {
			// don't care for the moment
			e.printStackTrace();
		}
		
		return null;
		
	}

	/**
	 * Builds and returns an Set containing one Access condition allowing access
	 * for AT terminals to the specified datagroup.
	 * 
	 * @param dgNr
	 * @return
	 */
	private Collection<SecCondition> getAccessRightReadEidDg(int dgNr) {
		HashSet<SecCondition> retVal = new HashSet<>();
		retVal.add(new TaSecurityCondition(TerminalType.AT,
				new RelativeAuthorization(CertificateRole.TERMINAL,
						new BitField(38).flipBit(dgNr + 7))));
		return retVal;
	}

	/**
	 * Builds and returns an Set containing one Access condition allowing update
	 * of specified datagroup.
	 * 
	 * @param dgNr
	 * @return
	 */
	private Collection<SecCondition> getAccessRightUpdateEidDg(int dgNr) {
		HashSet<SecCondition> retVal = new HashSet<>();
		retVal.add(new TaSecurityCondition(TerminalType.AT,
				new RelativeAuthorization(CertificateRole.TERMINAL,
						new BitField(38).flipBit(54-dgNr))));
		return retVal;
	}

	/**
	 * Build the content of EF.CardAccess.
	 * <p/>
	 * The default implementation shall use the returned SecInfos from the
	 * protocols and build EF.CardAccess accordingly. In order to provide a
	 * different EF.CardAccess this method may be overriden.
	 * 
	 * @return
	 */
	protected TlvDataObjectContainer getEfCardAccessContent() {
					
		// create EF.CardAccess
		TlvDataObjectContainer efCardAccessContent = new TlvDataObjectContainer();
		ConstructedTlvDataObject securityInfos = new ConstructedTlvDataObject(
				new TlvTag(Asn1.SET));
		efCardAccessContent.addTlvDataObject(securityInfos);

		// add all SecInfos from Protocols
		for (Protocol curProtocol : getProtocolList()) {
			for (TlvDataObject curSecInfo : curProtocol.getSecInfos()) {
				securityInfos.addTlvDataObject(curSecInfo);	
			}
		}
		
		return efCardAccessContent;
	}

	@Override
	public List<Protocol> getProtocolList() {
		if (protocols == null) {
			protocols = new ArrayList<>();
			
			/* load PACE protocol */
			PaceProtocol paceProtocol = new PaceProtocol();
			paceProtocol.init();
			protocols.add(paceProtocol);
	
			/* load FM protocol */
			FileProtocol fileManagementProtocol = new FileProtocol();
			fileManagementProtocol.init();
			protocols.add(fileManagementProtocol);
	
			/* load TA protocol */
			TaProtocol taProtocol = new TaProtocol();
			taProtocol.init();
			protocols.add(taProtocol);
			
			/* load RI protocol */
			RiProtocol riProtocol = new RiProtocol();
			protocols.add(riProtocol);
			
			/* load CA protocol */
			CaProtocol caProtocol = new CaProtocol();
			caProtocol.init();
			protocols.add(caProtocol);
			
			/* load AUX protocol */
			protocols.add(new AuxProtocol());
			
			/* load PIN-Management protocol */
			PinProtocol pinProtocol = new PinProtocol();
			pinProtocol.init();
			protocols.add(pinProtocol);
			
			/* let subclasses extend the protocol list */
			extendProtocolList();
		}
		return protocols;
	}

	/**
	 * This method is intended to be overriden by subclasses in order to extend the protocol list. 
	 * <p/>
	 * It is called immediately after the list of protocols (in {@link #protocols}) is created and before it is returned the first time.
	 */
	protected void extendProtocolList() {
	}

}
