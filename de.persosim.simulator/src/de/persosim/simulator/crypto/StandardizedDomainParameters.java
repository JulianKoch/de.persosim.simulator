package de.persosim.simulator.crypto;

import java.math.BigInteger;
import java.security.spec.ECFieldFp;
import java.security.spec.ECPoint;
import java.security.spec.EllipticCurve;
import java.util.HashMap;

import de.persosim.simulator.protocols.TR03110;
import de.persosim.simulator.tlv.ConstructedTlvDataObject;
import de.persosim.simulator.tlv.PrimitiveTlvDataObject;
import de.persosim.simulator.tlv.TlvConstants;
import de.persosim.simulator.utils.HexString;
import de.persosim.simulator.utils.Utils;

/**
 * This class provides static access to PACE standardized domain parameters.
 * 
 * @author slutters
 *
 */
public class StandardizedDomainParameters {
	public static final byte[] OID = Utils.appendBytes(TR03110.id_BSI, (byte) 0x01, (byte) 0x02);
	
	public static final int NO_OF_STANDARDIZED_DOMAIN_PARAMETERS = 32;
	
	// START DH
	static final BigInteger MODP_1024_160_PRIME = new BigInteger("B10B8F96A080E01DDE92DE5EAE5D54EC52C99FBCFB06A3C69A6A9DCA52D23B616073E28675A23D189838EF1E2EE652C013ECB4AEA906112324975C3CD49B83BFACCBDD7D90C4BD7098488E9C219A73724EFFD6FAE5644738FAA31A4FF55BCCC0A151AF5F0DC8B4BD45BF37DF365C1A65E68CFDA76D4DA708DF1FB2BC2E4A4371", 16);
	static final BigInteger MODP_1024_160_GENERATOR = new BigInteger("A4D1CBD5C3FD34126765A442EFB99905F8104DD258AC507FD6406CFF14266D31266FEA1E5C41564B777E690F5504F213160217B4B01B886A5E91547F9E2749F4D7FBD7D3B9A92EE1909D0D2263F80A76A6A24C087A091F531DBF0A0169B6A28AD662A4D18E73AFA32D779D5918D08BC8858F4DCEF97C2A24855E6EEB22B3B2E5",16);
	static final BigInteger MODP_1024_160_ORDER = new BigInteger("F518AA8781A8DF278ABA4E7D64B7CB9D49462353",16);

	static final BigInteger MODP_2048_224_PRIME = new BigInteger("AD107E1E9123A9D0D660FAA79559C51FA20D64E5683B9FD1B54B1597B61D0A75E6FA141DF95A56DBAF9A3C407BA1DF15EB3D688A309C180E1DE6B85A1274A0A66D3F8152AD6AC2129037C9EDEFDA4DF8D91E8FEF55B7394B7AD5B7D0B6C12207C9F98D11ED34DBF6C6BA0B2C8BBC27BE6A00E0A0B9C49708B3BF8A317091883681286130BC8985DB1602E714415D9330278273C7DE31EFDC7310F7121FD5A07415987D9ADC0A486DCDF93ACC44328387315D75E198C641A480CD86A1B9E587E8BE60E69CC928B2B9C52172E413042E9B23F10B0E16E79763C9B53DCF4BA80A29E3FB73C16B8E75B97EF363E2FFA31F71CF9DE5384E71B81C0AC4DFFE0C10E64F", 16);
	static final BigInteger MODP_2048_224_GENERATOR = new BigInteger("AC4032EF4F2D9AE39DF30B5C8FFDAC506CDEBE7B89998CAF74866A08CFE4FFE3A6824A4E10B9A6F0DD921F01A70C4AFAAB739D7700C29F52C57DB17C620A8652BE5E9001A8D66AD7C17669101999024AF4D027275AC1348BB8A762D0521BC98AE247150422EA1ED409939D54DA7460CDB5F6C6B250717CBEF180EB34118E98D119529A45D6F834566E3025E316A330EFBB77A86F0C1AB15B051AE3D428C8F8ACB70A8137150B8EEB10E183EDD19963DDD9E263E4770589EF6AA21E7F5F2FF381B539CCE3409D13CD566AFBB48D6C019181E1BCFE94B30269EDFE72FE9B6AA4BD7B5A0F1C71CFFF4C19C418E1F6EC017981BC087F2A7065B384B890D3191F2BFA",16);
	static final BigInteger MODP_2048_224_ORDER = new BigInteger("801C0D34C58D93FE997177101F80535A4738CEBCBF389A99B36371EB", 16);
	
	static final BigInteger MODP_2048_256_PRIME = new BigInteger("87A8E61DB4B6663CFFBBD19C651959998CEEF608660DD0F25D2CEED4435E3B00E00DF8F1D61957D4FAF7DF4561B2AA3016C3D91134096FAA3BF4296D830E9A7C209E0C6497517ABD5A8A9D306BCF67ED91F9E6725B4758C022E0B1EF4275BF7B6C5BFC11D45F9088B941F54EB1E59BB8BC39A0BF12307F5C4FDB70C581B23F76B63ACAE1CAA6B7902D52526735488A0EF13C6D9A51BFA4AB3AD8347796524D8EF6A167B5A41825D967E144E5140564251CCACB83E6B486F6B3CA3F7971506026C0B857F689962856DED4010ABD0BE621C3A3960A54E710C375F26375D7014103A4B54330C198AF126116D2276E11715F693877FAD7EF09CADB094AE91E1A1597", 16);
	static final BigInteger MODP_2048_256_GENERATOR = new BigInteger("3FB32C9B73134D0B2E77506660EDBD484CA7B18F21EF205407F4793A1A0BA12510DBC15077BE463FFF4FED4AAC0BB555BE3A6C1B0C6B47B1BC3773BF7E8C6F62901228F8C28CBB18A55AE31341000A650196F931C77A57F2DDF463E5E9EC144B777DE62AAAB8A8628AC376D282D6ED3864E67982428EBC831D14348F6F2F9193B5045AF2767164E1DFC967C1FB3F2E55A4BD1BFFE83B9C80D052B985D182EA0ADB2A3B7313D3FE14C8484B1E052588B9B7D2BBD2DF016199ECD06E1557CD0915B3353BBB64E0EC377FD028370DF92B52C7891428CDC67EB6184B523D1DB246C32F63078490F00EF8D647D148D47954515E2327CFEF98C582664B4C0F6CC41659",16);
	static final BigInteger MODP_2048_256_ORDER = new BigInteger("8CF83642A709A097B447997640129DA299B1A47D1EB3750BA308B0FE64F5FBD3",16);
	// END DH
	
	
	
	// START ECDH
	
	// parameters of TeleTrust named ec curves taken from
	// http://www.ecc-brainpool.org/download/draft-lochter-pkix-brainpool-ecc-00.txt
	static final BigInteger BRAINPOOLP192R1_P = new BigInteger("C302F41D932A36CDA7A3463093D18DB78FCE476DE1A86297",16);
	static final BigInteger BRAINPOOLP192R1_A = new BigInteger("6A91174076B1E0E19C39C031FE8685C1CAE040E5C69A28EF",16);
	static final BigInteger BRAINPOOLP192R1_B = new BigInteger("469A28EF7C28CCA3DC721D044F4496BCCA7EF4146FBF25C9",16);
	static final BigInteger BRAINPOOLP192R1_X = new BigInteger("C0A0647EAAB6A48753B033C56CB0F0900A2F5C4853375FD6",16);
	static final BigInteger BRAINPOOLP192R1_Y = new BigInteger("14B690866ABD5BB88B5F4828C1490002E6773FA2FA299B8F",16);
	static final BigInteger BRAINPOOLP192R1_Q = new BigInteger("C302F41D932A36CDA7A3462F9E9E916B5BE8F1029AC4ACC1",16);
	static final BigInteger BRAINPOOLP192R1_H = new BigInteger("01",16);
	
	static final BigInteger BRAINPOOLP224R1_P = new BigInteger("D7C134AA264366862A18302575D1D787B09F075797DA89F57EC8C0FF",16);
	static final BigInteger BRAINPOOLP224R1_A = new BigInteger("68A5E62CA9CE6C1C299803A6C1530B514E182AD8B0042A59CAD29F43",16);
	static final BigInteger BRAINPOOLP224R1_B = new BigInteger("2580F63CCFE44138870713B1A92369E33E2135D266DBB372386C400B",16);
	static final BigInteger BRAINPOOLP224R1_X = new BigInteger("D9029AD2C7E5CF4340823B2A87DC68C9E4CE3174C1E6EFDEE12C07D",16);
	static final BigInteger BRAINPOOLP224R1_Y = new BigInteger("58AA56F772C0726F24C6B89E4ECDAC24354B9E99CAA3F6D3761402CD",16);
	static final BigInteger BRAINPOOLP224R1_Q = new BigInteger("D7C134AA264366862A18302575D0FB98D116BC4B6DDEBCA3A5A7939F",16);
	static final BigInteger BRAINPOOLP224R1_H = new BigInteger("01",16);
	
	static final BigInteger BRAINPOOLP256R1_P = new BigInteger("A9FB57DBA1EEA9BC3E660A909D838D726E3BF623D52620282013481D1F6E5377",16);
	static final BigInteger BRAINPOOLP256R1_A = new BigInteger("7D5A0975FC2C3057EEF67530417AFFE7FB8055C126DC5C6CE94A4B44F330B5D9",16);
	static final BigInteger BRAINPOOLP256R1_B = new BigInteger("26DC5C6CE94A4B44F330B5D9BBD77CBF958416295CF7E1CE6BCCDC18FF8C07B6",16);
	static final BigInteger BRAINPOOLP256R1_X = new BigInteger("8BD2AEB9CB7E57CB2C4B482FFC81B7AFB9DE27E1E3BD23C23A4453BD9ACE3262",16);
	static final BigInteger BRAINPOOLP256R1_Y = new BigInteger("547EF835C3DAC4FD97F8461A14611DC9C27745132DED8E545C1D54C72F046997",16);
	static final BigInteger BRAINPOOLP256R1_Q = new BigInteger("A9FB57DBA1EEA9BC3E660A909D838D718C397AA3B561A6F7901E0E82974856A7",16);
	static final BigInteger BRAINPOOLP256R1_H = new BigInteger("01",16);
	
	static final BigInteger BRAINPOOLP320R1_P = new BigInteger("D35E472036BC4FB7E13C785ED201E065F98FCFA6F6F40DEF4F92B9EC7893EC28FCD412B1F1B32E27",16);
	static final BigInteger BRAINPOOLP320R1_A = new BigInteger("3EE30B568FBAB0F883CCEBD46D3F3BB8A2A73513F5EB79DA66190EB085FFA9F492F375A97D860EB4",16);
	static final BigInteger BRAINPOOLP320R1_B = new BigInteger("520883949DFDBC42D3AD198640688A6FE13F41349554B49ACC31DCCD884539816F5EB4AC8FB1F1A6",16);
	static final BigInteger BRAINPOOLP320R1_X = new BigInteger("43BD7E9AFB53D8B85289BCC48EE5BFE6F20137D10A087EB6E7871E2A10A599C710AF8D0D39E20611",16);
	static final BigInteger BRAINPOOLP320R1_Y = new BigInteger("14FDD05545EC1CC8AB4093247F77275E0743FFED117182EAA9C77877AAAC6AC7D35245D1692E8EE1",16);
	static final BigInteger BRAINPOOLP320R1_Q = new BigInteger("D35E472036BC4FB7E13C785ED201E065F98FCFA5B68F12A32D482EC7EE8658E98691555B44C59311",16);
	static final BigInteger BRAINPOOLP320R1_H = new BigInteger("01",16);
	
	static final BigInteger BRAINPOOLP384R1_P = new BigInteger("8CB91E82A3386D280F5D6F7E50E641DF152F7109ED5456B412B1DA197FB71123ACD3A729901D1A71874700133107EC53",16);
	static final BigInteger BRAINPOOLP384R1_A = new BigInteger("7BC382C63D8C150C3C72080ACE05AFA0C2BEA28E4FB22787139165EFBA91F90F8AA5814A503AD4EB04A8C7DD22CE2826",16);
	static final BigInteger BRAINPOOLP384R1_B = new BigInteger("04A8C7DD22CE28268B39B55416F0447C2FB77DE107DCD2A62E880EA53EEB62D57CB4390295DBC9943AB78696FA504C11",16);
	static final BigInteger BRAINPOOLP384R1_X = new BigInteger("1D1C64F068CF45FFA2A63A81B7C13F6B8847A3E77EF14FE3DB7FCAFE0CBD10E8E826E03436D646AAEF87B2E247D4AF1E",16);
	static final BigInteger BRAINPOOLP384R1_Y = new BigInteger("8ABE1D7520F9C2A45CB1EB8E95CFD55262B70B29FEEC5864E19C054FF99129280E4646217791811142820341263C5315",16);
	static final BigInteger BRAINPOOLP384R1_Q = new BigInteger("8CB91E82A3386D280F5D6F7E50E641DF152F7109ED5456B31F166E6CAC0425A7CF3AB6AF6B7FC3103B883202E9046565",16);
	static final BigInteger BRAINPOOLP384R1_H = new BigInteger("01",16);
	
	static final BigInteger BRAINPOOLP512R1_P = new BigInteger("AADD9DB8DBE9C48B3FD4E6AE33C9FC07CB308DB3B3C9D20ED6639CCA703308717D4D9B009BC66842AECDA12AE6A380E62881FF2F2D82C68528AA6056583A48F3",16);
	static final BigInteger BRAINPOOLP512R1_A = new BigInteger("7830A3318B603B89E2327145AC234CC594CBDD8D3DF91610A83441CAEA9863BC2DED5D5AA8253AA10A2EF1C98B9AC8B57F1117A72BF2C7B9E7C1AC4D77FC94CA",16);
	static final BigInteger BRAINPOOLP512R1_B = new BigInteger("3DF91610A83441CAEA9863BC2DED5D5AA8253AA10A2EF1C98B9AC8B57F1117A72BF2C7B9E7C1AC4D77FC94CADC083E67984050B75EBAE5DD2809BD638016F723",16);
	static final BigInteger BRAINPOOLP512R1_X = new BigInteger("81AEE4BDD82ED9645A21322E9C4C6A9385ED9F70B5D916C1B43B62EEF4D0098EFF3B1F78E2D0D48D50D1687B93B97D5F7C6D5047406A5E688B352209BCB9F822",16);
	static final BigInteger BRAINPOOLP512R1_Y = new BigInteger("7DDE385D566332ECC0EABFA9CF7822FDF209F70024A57B1AA000C55B881F8111B2DCDE494A5F485E5BCA4BD88A2763AED1CA2B2FA8F0540678CD1E0F3AD80892",16);
	static final BigInteger BRAINPOOLP512R1_Q = new BigInteger("AADD9DB8DBE9C48B3FD4E6AE33C9FC07CB308DB3B3C9D20ED6639CCA70330870553E5C414CA92619418661197FAC10471DB1D381085DDADDB58796829CA90069",16);
	static final BigInteger BRAINPOOLP512R1_H = new BigInteger("01",16);
	
	// parameters of NIST named ec curves extracted from Bouncy Castle objects returned by
	// calling e.g. NISTNamedCurves.getByName("P-192") or TeleTrusTNamedCurves.getByName("brainpoolp192r1")
	static final BigInteger P192_P = new BigInteger("00FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEFFFFFFFFFFFFFFFF",16);
	static final BigInteger P192_A = new BigInteger("00FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEFFFFFFFFFFFFFFFC",16);
	static final BigInteger P192_B = new BigInteger("64210519E59C80E70FA7E9AB72243049FEB8DEECC146B9B1",16);
	static final BigInteger P192_X = new BigInteger("188DA80EB03090F67CBF20EB43A18800F4FF0AFD82FF1012",16);
	static final BigInteger P192_Y = new BigInteger("07192B95FFC8DA78631011ED6B24CDD573F977A11E794811",16);
	static final BigInteger P192_Q = new BigInteger("00FFFFFFFFFFFFFFFFFFFFFFFF99DEF836146BC9B1B4D22831",16);
	static final BigInteger P192_H = new BigInteger("01",16);
	
	static final BigInteger P224_P = new BigInteger("00FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF000000000000000000000001",16);
	static final BigInteger P224_A = new BigInteger("00FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEFFFFFFFFFFFFFFFFFFFFFFFE",16);
	static final BigInteger P224_B = new BigInteger("00B4050A850C04B3ABF54132565044B0B7D7BFD8BA270B39432355FFB4",16);
	static final BigInteger P224_X = new BigInteger("00B70E0CBD6BB4BF7F321390B94A03C1D356C21122343280D6115C1D21",16);
	static final BigInteger P224_Y = new BigInteger("00BD376388B5F723FB4C22DFE6CD4375A05A07476444D5819985007E34",16);
	static final BigInteger P224_Q = new BigInteger("00FFFFFFFFFFFFFFFFFFFFFFFFFFFF16A2E0B8F03E13DD29455C5C2A3D",16);
	static final BigInteger P224_H = new BigInteger("01",16);
	
	static final BigInteger P256_P = new BigInteger("00FFFFFFFF00000001000000000000000000000000FFFFFFFFFFFFFFFFFFFFFFFF",16);
	static final BigInteger P256_A = new BigInteger("00FFFFFFFF00000001000000000000000000000000FFFFFFFFFFFFFFFFFFFFFFFC",16);
	static final BigInteger P256_B = new BigInteger("5AC635D8AA3A93E7B3EBBD55769886BC651D06B0CC53B0F63BCE3C3E27D2604B",16);
	static final BigInteger P256_X = new BigInteger("6B17D1F2E12C4247F8BCE6E563A440F277037D812DEB33A0F4A13945D898C296",16);
	static final BigInteger P256_Y = new BigInteger("4FE342E2FE1A7F9B8EE7EB4A7C0F9E162BCE33576B315ECECBB6406837BF51F5",16);
	static final BigInteger P256_Q = new BigInteger("00FFFFFFFF00000000FFFFFFFFFFFFFFFFBCE6FAADA7179E84F3B9CAC2FC632551",16);
	static final BigInteger P256_H = new BigInteger("01",16);
	
	static final BigInteger P384_P = new BigInteger("00FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEFFFFFFFF0000000000000000FFFFFFFF",16);
	static final BigInteger P384_A = new BigInteger("00FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEFFFFFFFF0000000000000000FFFFFFFC",16);
	static final BigInteger P384_B = new BigInteger("00B3312FA7E23EE7E4988E056BE3F82D19181D9C6EFE8141120314088F5013875AC656398D8A2ED19D2A85C8EDD3EC2AEF",16);
	static final BigInteger P384_X = new BigInteger("00AA87CA22BE8B05378EB1C71EF320AD746E1D3B628BA79B9859F741E082542A385502F25DBF55296C3A545E3872760AB7",16);
	static final BigInteger P384_Y = new BigInteger("3617DE4A96262C6F5D9E98BF9292DC29F8F41DBD289A147CE9DA3113B5F0B8C00A60B1CE1D7E819D7A431D7C90EA0E5F",16);
	static final BigInteger P384_Q = new BigInteger("00FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFC7634D81F4372DDF581A0DB248B0A77AECEC196ACCC52973",16);
	static final BigInteger P384_H = new BigInteger("01",16);
	
	static final BigInteger P521_P = new BigInteger("01FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF",16);
	static final BigInteger P521_A = new BigInteger("01FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFC",16);
	static final BigInteger P521_B = new BigInteger("51953EB9618E1C9A1F929A21A0B68540EEA2DA725B99B315F3B8B489918EF109E156193951EC7E937B1652C0BD3BB1BF073573DF883D2C34F1EF451FD46B503F00",16);
	static final BigInteger P521_X = new BigInteger("00C6858E06B70404E9CD9E3ECB662395B4429C648139053FB521F828AF606B4D3DBAA14B5E77EFE75928FE1DC127A2FFA8DE3348B3C1856A429BF97E7E31C2E5BD66",16);
	static final BigInteger P521_Y = new BigInteger("011839296A789A3BC0045C8A5FB42C7D1BD998F54449579B446817AFBD17273E662C97EE72995EF42640C550B9013FAD0761353C7086A272C24088BE94769FD16650",16);
	static final BigInteger P521_Q = new BigInteger("01FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFA51868783BF2F966B7FCC0148F709A5D03BB5C9B8899C47AEBB6FB71E91386409",16);
	static final BigInteger P521_H = new BigInteger("01",16);
	// END ECDH

	/*--------------------------------------------------------------------------------*/
	
	static public DomainParameterSet getDomainParameterSetById(int id){
		switch (id){
		case 0:  // fallthrough
		case 1:  // fallthrough
		case 2:  // fallthrough
		case 3:  // fallthrough
		case 4:  // fallthrough
		case 5:  // fallthrough
		case 6:  // fallthrough
		case 7:
			return null; //IMPL add missing standardized domain parameters
		case 8:
			return new DomainParameterSetEcdh(generateCurveFrom(P192_P, P192_A, P192_B), new ECPoint(P192_X, P192_Y), P192_Q, P192_H.intValue());
		case 9:
			return new DomainParameterSetEcdh(generateCurveFrom(BRAINPOOLP192R1_P, BRAINPOOLP192R1_A, BRAINPOOLP192R1_B), new ECPoint(BRAINPOOLP192R1_X, BRAINPOOLP192R1_Y), BRAINPOOLP192R1_Q, BRAINPOOLP192R1_H.intValue());
		case 10:
			return new DomainParameterSetEcdh(generateCurveFrom(P224_P, P224_A, P224_B), new ECPoint(P224_X, P224_Y), P224_Q, P224_H.intValue());
		case 11:
			return new DomainParameterSetEcdh(generateCurveFrom(BRAINPOOLP224R1_P, BRAINPOOLP224R1_A, BRAINPOOLP224R1_B), new ECPoint(BRAINPOOLP224R1_X, BRAINPOOLP224R1_Y), BRAINPOOLP224R1_Q, BRAINPOOLP224R1_H.intValue());
		case 12:
			return new DomainParameterSetEcdh(generateCurveFrom(P256_P, P256_A, P256_B), new ECPoint(P256_X, P256_Y), P256_Q, P256_H.intValue());
		case 13:
			return new DomainParameterSetEcdh(generateCurveFrom(BRAINPOOLP256R1_P, BRAINPOOLP256R1_A, BRAINPOOLP256R1_B), new ECPoint(BRAINPOOLP256R1_X, BRAINPOOLP256R1_Y), BRAINPOOLP256R1_Q, BRAINPOOLP256R1_H.intValue());
		case 14:
			return new DomainParameterSetEcdh(generateCurveFrom(BRAINPOOLP320R1_P, BRAINPOOLP320R1_A, BRAINPOOLP320R1_B), new ECPoint(BRAINPOOLP320R1_X, BRAINPOOLP320R1_Y), BRAINPOOLP320R1_Q, BRAINPOOLP320R1_H.intValue());
		case 15:
			return new DomainParameterSetEcdh(generateCurveFrom(P384_P, P384_A, P384_B), new ECPoint(P384_X, P384_Y), P384_Q, P384_H.intValue());
		case 16:
			return new DomainParameterSetEcdh(generateCurveFrom(BRAINPOOLP384R1_P, BRAINPOOLP384R1_A, BRAINPOOLP384R1_B), new ECPoint(BRAINPOOLP384R1_X, BRAINPOOLP384R1_Y), BRAINPOOLP384R1_Q, BRAINPOOLP384R1_H.intValue());
		case 17:
			return new DomainParameterSetEcdh(generateCurveFrom(BRAINPOOLP512R1_P, BRAINPOOLP512R1_A, BRAINPOOLP512R1_B), new ECPoint(BRAINPOOLP512R1_X, BRAINPOOLP512R1_Y), BRAINPOOLP512R1_Q, BRAINPOOLP512R1_H.intValue());
		case 18:
			return new DomainParameterSetEcdh(generateCurveFrom(P521_P, P521_A, P521_B), new ECPoint(P521_X, P521_Y), P521_Q, P521_H.intValue());
		case 19:  // fallthrough
		case 20:  // fallthrough
		case 21:  // fallthrough
		case 22:  // fallthrough
		case 23:  // fallthrough
		case 24:  // fallthrough
		case 25:  // fallthrough
		case 26:  // fallthrough
		case 27:  // fallthrough
		case 28:  // fallthrough
		case 29:  // fallthrough
		case 30:  // fallthrough
		case 31:
			return null;
		default:
			throw new IllegalArgumentException("id for standardized domain parameters must be > 0 and < " + NO_OF_STANDARDIZED_DOMAIN_PARAMETERS);
		}
	}
	
	/**
	 * This method creates an object of {@link EllipticCurve} from its basic parameters.
	 * @param p prime p specifying the base field
	 * @param a coefficient A defining the curve
	 * @param b coefficient B defining the curve
	 * @return the corresponding {@link EllipticCurve} object
	 */
	public static EllipticCurve generateCurveFrom(BigInteger p, BigInteger a, BigInteger b) {
		return new EllipticCurve(new ECFieldFp(p), a, b);
	}

	
	private static HashMap<String, Integer> algIdentifierMapping = null;

	private static void initAlgIdentifierMapping() {
		algIdentifierMapping = new HashMap<>();
		
		algIdentifierMapping.put("3081BD06072A8648CE3D02013081B1020101302406072A8648CE3D0101021900FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEFFFFFFFFFFFFFFFF3035041900FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEFFFFFFFFFFFFFFFC041864210519E59C80E70FA7E9AB72243049FEB8DEECC146B9B1043104188DA80EB03090F67CBF20EB43A18800F4FF0AFD82FF101207192B95FFC8DA78631011ED6B24CDD573F977A11E794811021900FFFFFFFFFFFFFFFFFFFFFFFF99DEF836146BC9B1B4D22831020101", 0x08);
		algIdentifierMapping.put("3081BC06072A8648CE3D02013081B0020101302406072A8648CE3D0101021900C302F41D932A36CDA7A3463093D18DB78FCE476DE1A86297303404186A91174076B1E0E19C39C031FE8685C1CAE040E5C69A28EF0418469A28EF7C28CCA3DC721D044F4496BCCA7EF4146FBF25C9043104C0A0647EAAB6A48753B033C56CB0F0900A2F5C4853375FD614B690866ABD5BB88B5F4828C1490002E6773FA2FA299B8F021900C302F41D932A36CDA7A3462F9E9E916B5BE8F1029AC4ACC1020101", 0x09);
		algIdentifierMapping.put("3081D606072A8648CE3D02013081CA020101302806072A8648CE3D0101021D00FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF000000000000000000000001303E041D00FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEFFFFFFFFFFFFFFFFFFFFFFFE041D00B4050A850C04B3ABF54132565044B0B7D7BFD8BA270B39432355FFB4043904B70E0CBD6BB4BF7F321390B94A03C1D356C21122343280D6115C1D21BD376388B5F723FB4C22DFE6CD4375A05A07476444D5819985007E34021D00FFFFFFFFFFFFFFFFFFFFFFFFFFFF16A2E0B8F03E13DD29455C5C2A3D020101", 0x0A);
		algIdentifierMapping.put("3081D406072A8648CE3D02013081C8020101302806072A8648CE3D0101021D00D7C134AA264366862A18302575D1D787B09F075797DA89F57EC8C0FF303C041C68A5E62CA9CE6C1C299803A6C1530B514E182AD8B0042A59CAD29F43041C2580F63CCFE44138870713B1A92369E33E2135D266DBB372386C400B0439040D9029AD2C7E5CF4340823B2A87DC68C9E4CE3174C1E6EFDEE12C07D58AA56F772C0726F24C6B89E4ECDAC24354B9E99CAA3F6D3761402CD021D00D7C134AA264366862A18302575D0FB98D116BC4B6DDEBCA3A5A7939F020101", 0x0B);
		algIdentifierMapping.put("3081ED06072A8648CE3D02013081E1020101302C06072A8648CE3D0101022100FFFFFFFF00000001000000000000000000000000FFFFFFFFFFFFFFFFFFFFFFFF3045042100FFFFFFFF00000001000000000000000000000000FFFFFFFFFFFFFFFFFFFFFFFC04205AC635D8AA3A93E7B3EBBD55769886BC651D06B0CC53B0F63BCE3C3E27D2604B0441046B17D1F2E12C4247F8BCE6E563A440F277037D812DEB33A0F4A13945D898C2964FE342E2FE1A7F9B8EE7EB4A7C0F9E162BCE33576B315ECECBB6406837BF51F5022100FFFFFFFF00000000FFFFFFFFFFFFFFFFBCE6FAADA7179E84F3B9CAC2FC632551020101", 0x0C);
		algIdentifierMapping.put("3081EC06072A8648CE3D02013081E0020101302C06072A8648CE3D0101022100A9FB57DBA1EEA9BC3E660A909D838D726E3BF623D52620282013481D1F6E5377304404207D5A0975FC2C3057EEF67530417AFFE7FB8055C126DC5C6CE94A4B44F330B5D9042026DC5C6CE94A4B44F330B5D9BBD77CBF958416295CF7E1CE6BCCDC18FF8C07B60441048BD2AEB9CB7E57CB2C4B482FFC81B7AFB9DE27E1E3BD23C23A4453BD9ACE3262547EF835C3DAC4FD97F8461A14611DC9C27745132DED8E545C1D54C72F046997022100A9FB57DBA1EEA9BC3E660A909D838D718C397AA3B561A6F7901E0E82974856A7020101", 0x0D);
		algIdentifierMapping.put("3082011D06072A8648CE3D020130820110020101303406072A8648CE3D0101022900D35E472036BC4FB7E13C785ED201E065F98FCFA6F6F40DEF4F92B9EC7893EC28FCD412B1F1B32E27305404283EE30B568FBAB0F883CCEBD46D3F3BB8A2A73513F5EB79DA66190EB085FFA9F492F375A97D860EB40428520883949DFDBC42D3AD198640688A6FE13F41349554B49ACC31DCCD884539816F5EB4AC8FB1F1A604510443BD7E9AFB53D8B85289BCC48EE5BFE6F20137D10A087EB6E7871E2A10A599C710AF8D0D39E2061114FDD05545EC1CC8AB4093247F77275E0743FFED117182EAA9C77877AAAC6AC7D35245D1692E8EE1022900D35E472036BC4FB7E13C785ED201E065F98FCFA5B68F12A32D482EC7EE8658E98691555B44C59311020101", 0x0E);
		algIdentifierMapping.put("3082014F06072A8648CE3D020130820142020101303C06072A8648CE3D0101023100FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEFFFFFFFF0000000000000000FFFFFFFF3066043100FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEFFFFFFFF0000000000000000FFFFFFFC043100B3312FA7E23EE7E4988E056BE3F82D19181D9C6EFE8141120314088F5013875AC656398D8A2ED19D2A85C8EDD3EC2AEF046104AA87CA22BE8B05378EB1C71EF320AD746E1D3B628BA79B9859F741E082542A385502F25DBF55296C3A545E3872760AB73617DE4A96262C6F5D9E98BF9292DC29F8F41DBD289A147CE9DA3113B5F0B8C00A60B1CE1D7E819D7A431D7C90EA0E5F023100FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFC7634D81F4372DDF581A0DB248B0A77AECEC196ACCC52973020101", 0x0F);
		algIdentifierMapping.put("3082014D06072A8648CE3D020130820140020101303C06072A8648CE3D01010231008CB91E82A3386D280F5D6F7E50E641DF152F7109ED5456B412B1DA197FB71123ACD3A729901D1A71874700133107EC53306404307BC382C63D8C150C3C72080ACE05AFA0C2BEA28E4FB22787139165EFBA91F90F8AA5814A503AD4EB04A8C7DD22CE2826043004A8C7DD22CE28268B39B55416F0447C2FB77DE107DCD2A62E880EA53EEB62D57CB4390295DBC9943AB78696FA504C110461041D1C64F068CF45FFA2A63A81B7C13F6B8847A3E77EF14FE3DB7FCAFE0CBD10E8E826E03436D646AAEF87B2E247D4AF1E8ABE1D7520F9C2A45CB1EB8E95CFD55262B70B29FEEC5864E19C054FF99129280E4646217791811142820341263C53150231008CB91E82A3386D280F5D6F7E50E641DF152F7109ED5456B31F166E6CAC0425A7CF3AB6AF6B7FC3103B883202E9046565020101", 0x10);
		algIdentifierMapping.put("308201AF06072A8648CE3D0201308201A2020101304C06072A8648CE3D0101024100AADD9DB8DBE9C48B3FD4E6AE33C9FC07CB308DB3B3C9D20ED6639CCA703308717D4D9B009BC66842AECDA12AE6A380E62881FF2F2D82C68528AA6056583A48F330818404407830A3318B603B89E2327145AC234CC594CBDD8D3DF91610A83441CAEA9863BC2DED5D5AA8253AA10A2EF1C98B9AC8B57F1117A72BF2C7B9E7C1AC4D77FC94CA04403DF91610A83441CAEA9863BC2DED5D5AA8253AA10A2EF1C98B9AC8B57F1117A72BF2C7B9E7C1AC4D77FC94CADC083E67984050B75EBAE5DD2809BD638016F7230481810481AEE4BDD82ED9645A21322E9C4C6A9385ED9F70B5D916C1B43B62EEF4D0098EFF3B1F78E2D0D48D50D1687B93B97D5F7C6D5047406A5E688B352209BCB9F8227DDE385D566332ECC0EABFA9CF7822FDF209F70024A57B1AA000C55B881F8111B2DCDE494A5F485E5BCA4BD88A2763AED1CA2B2FA8F0540678CD1E0F3AD80892024100AADD9DB8DBE9C48B3FD4E6AE33C9FC07CB308DB3B3C9D20ED6639CCA70330870553E5C414CA92619418661197FAC10471DB1D381085DDADDB58796829CA90069020101", 0x11);
		algIdentifierMapping.put("308201B806072A8648CE3D0201308201AB020101304D06072A8648CE3D0101024201FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF308187044201FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFC044151953EB9618E1C9A1F929A21A0B68540EEA2DA725B99B315F3B8B489918EF109E156193951EC7E937B1652C0BD3BB1BF073573DF883D2C34F1EF451FD46B503F000481850400C6858E06B70404E9CD9E3ECB662395B4429C648139053FB521F828AF606B4D3DBAA14B5E77EFE75928FE1DC127A2FFA8DE3348B3C1856A429BF97E7E31C2E5BD66011839296A789A3BC0045C8A5FB42C7D1BD998F54449579B446817AFBD17273E662C97EE72995EF42640C550B9013FAD0761353C7086A272C24088BE94769FD16650024201FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFA51868783BF2F966B7FCC0148F709A5D03BB5C9B8899C47AEBB6FB71E91386409020101", 0x12); 
	}
	
	
	/**
	 * Simplify the given AlgorithmIdentifier using standardized domain
	 * parameters if possible
	 * <p/>
	 * Returns a new AlgorithmIdentifier describing the provided input using OID
	 * bsi-de 1 2 and an integer identifying the used domain parameter set.
	 * <p/>
	 * If the provided input does not match any known standardized domain
	 * parameters the input is returned without further checking.
	 * 
	 * @param algIdentifier
	 * @return
	 */
	public static ConstructedTlvDataObject simplifyAlgorithmIdentifier(
			ConstructedTlvDataObject algIdentifier) {
		if (algIdentifierMapping == null) {
			initAlgIdentifierMapping();
		}
		
		//get DomainParameterId from HashMap
		String algIdHexString = HexString.encode(algIdentifier.toByteArray());
		if (algIdentifierMapping.containsKey(algIdHexString)) {
			ConstructedTlvDataObject newAlgIdentifier = new ConstructedTlvDataObject(TlvConstants.TAG_SEQUENCE);
			newAlgIdentifier.addTlvDataObject(new PrimitiveTlvDataObject(TlvConstants.TAG_OID, OID));
			newAlgIdentifier.addTlvDataObject(new PrimitiveTlvDataObject(TlvConstants.TAG_INTEGER, new byte[] {(byte) algIdentifierMapping.get(algIdHexString).intValue()}));
			return newAlgIdentifier;
		}
		
		//fallthrough, return input
		return algIdentifier;
	}
	
}
