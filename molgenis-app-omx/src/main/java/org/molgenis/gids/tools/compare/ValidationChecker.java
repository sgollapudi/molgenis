package org.molgenis.gids.tools.compare;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.io.IOUtils;
import org.molgenis.io.csv.CsvReader;
import org.molgenis.io.excel.ExcelReader;
import org.molgenis.io.excel.ExcelSheetReader;
import org.molgenis.util.tuple.Tuple;

/**
 * This script is created to compare 2 excelfiles. The files are given via the
 * arguments
 */

public class ValidationChecker
{

	/**
	 * @param args
	 * @throws IOException
	 * 
	 */
	public BufferedWriter logger = null;
	private final static String IDENTIFIER = "id_sample";

	public void check(String file1, String file2, BufferedWriter logger) throws IOException
	{
		this.logger = logger;
		LinkedHashMap<String, String> hashCheckedValues = new LinkedHashMap<String, String>();

		// Make Object Reference
		ValidationFile excelfile = new ValidationFile();
		ExcelReader excelReaderReferenceFile = new ExcelReader(new File(file1));

		ExcelSheetReader excelSheetReaderReferenceFile = excelReaderReferenceFile.getSheet(0);
		excelfile.bla(excelSheetReaderReferenceFile, IDENTIFIER);

		// Make Object FileToCompare
		ValidationFile csvFile = new ValidationFile();
		CsvReader csvReaderFileToCompare = new CsvReader(new File(file2));

		csvFile.bla(csvReaderFileToCompare, IDENTIFIER);
		boolean noUniqueColums = false;
		// Make list for shared headers

		List<String> listOfSharedHeaders = new ArrayList<String>();
		// Compare the headers of Reference file with fileToCompare
		// Add Reference headers to the listOfSharedHeaders
		System.out.println("### Unique columns");
		for (String o : excelfile.getListOfHeaders())
		{
			listOfSharedHeaders.add(o);
			if (!csvFile.getListOfHeaders().contains(o))
			{
				System.out.println("In file1: " + o);
				noUniqueColums = true;
			}
		}

		// Compare the headers of fileToCompare file with Reference
		// Add Reference headers to the listOfSharedHeaders
		for (String o : csvFile.getListOfHeaders())
		{
			if (!listOfSharedHeaders.contains(o))
			{
				listOfSharedHeaders.add(o);
			}
			if (!excelfile.getListOfHeaders().contains(o))
			{
				System.out.println("In file2: " + o);
				noUniqueColums = true;
			}
		}

		if (noUniqueColums == false)
		{
			System.out.println("###There are no added/deleted columns\n");
		}

		System.out.println("\n###Comparing the values ");
		System.out.println("Sample ID\tFeature\tFile1\tFile2");

		try
		{
			for (Entry<String, Tuple> entry : excelfile.getHash().entrySet())
			{
				if (csvFile.getHash().get(entry.getValue().getString(IDENTIFIER)) != null)
				{
					compareRows(csvFile.getHash().get(entry.getValue().getString(IDENTIFIER)), entry.getValue(),
							listOfSharedHeaders, hashCheckedValues, logger);
				}

			}

			for (Entry<String, Tuple> entry : csvFile.getHash().entrySet())
			{
				if (excelfile.getHash().get(entry.getValue().getString(IDENTIFIER)) != null)
				{
					compareRows(entry.getValue(), excelfile.getHash().get(entry.getValue().getString(IDENTIFIER)),
							listOfSharedHeaders, hashCheckedValues, logger);
				}

			}

			System.out.println("\n###Unique samples in file1");
			for (Entry<String, Tuple> entry : excelfile.getHash().entrySet())
			{
				if (!csvFile.getHash().containsKey(entry.getKey()))
				{
					System.out.println(entry.getKey());
				}
			}
			System.out.println("\n###Unique samples in file2 ");
			for (Entry<String, Tuple> entry : csvFile.getHash().entrySet())
			{
				if (!excelfile.getHash().containsKey(entry.getKey()))
				{
					System.out.println(entry.getKey());
				}
			}

		}
		finally
		{
			IOUtils.closeQuietly(csvReaderFileToCompare);
			IOUtils.closeQuietly(excelReaderReferenceFile);
		}
	}

	public static void compareRows(Tuple firstTuple, Tuple secondTuple, List<String> listOfHeaders,
			LinkedHashMap<String, String> hashCheckedValues, BufferedWriter logger) throws IOException
	{

		for (String e : listOfHeaders)
		{

			if (firstTuple.getString(e) == null && secondTuple.getString(e) == null)
			{

			}
			else if (firstTuple.getString(e) != null && secondTuple.getString(e) == null)
			{
				if (!hashCheckedValues.containsKey(firstTuple.getString(IDENTIFIER) + e))
				{
					System.out.println("### " + firstTuple.getString(IDENTIFIER) + "\t" + e + "\t"
							+ (secondTuple.getString(e) == null ? ("\tAdded") : secondTuple.getString(e)) + "\t"
							+ firstTuple.getString(e));
					hashCheckedValues.put(firstTuple.getString(IDENTIFIER) + e, secondTuple.getString(e));

				}

			}
			else if (firstTuple.getString(e) == null && secondTuple.getString(e) != null)
			{
				if (!hashCheckedValues.containsKey(firstTuple.getString(IDENTIFIER) + e))
				{
					System.out.println(firstTuple.getString(IDENTIFIER)
							+ "\t"
							+ e
							+ "\t"
							+ (secondTuple.getString(e) == null ? ("\tAdded in the " + "file2") : "|"
									+ secondTuple.getString(e)) + "|\t|" + firstTuple.getString(e) + "|");
					hashCheckedValues.put(firstTuple.getString(IDENTIFIER) + e, secondTuple.getString(e));
				}
			}
			else
			{
				if (!firstTuple.getString(e).equals(secondTuple.getString(e)))
				{
					if (!hashCheckedValues.containsKey(firstTuple.getString(IDENTIFIER) + e))
					{

						hashCheckedValues.put(firstTuple.getString(IDENTIFIER) + e, secondTuple.getString(e));
						System.out.println(firstTuple.getString(IDENTIFIER)
								+ "\t"
								+ e
								+ "\t"
								+ (secondTuple.getString(e) == null ? ("\tAdded in the " + "Reference") : "|"
										+ secondTuple.getString(e)) + "|\t|" + firstTuple.getString(e) + "|");
					}

				}

			}

		}

	}
}
